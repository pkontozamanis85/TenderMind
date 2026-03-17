package TenderMind.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import TenderMind.dtos.SearchDto;
import TenderMind.model.Bid;
import TenderMind.repositories.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for paginated data ingestion from the KHMDHS API.
 * Implements path-agnostic JSON mapping for CPV, NUTS, and Budgetary data.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BidRepository bidRepository;

    private static final String BASE_URL = "https://cerpp.eprocurement.gov.gr/khmdhs-opendata/notice?page=";
    private static final int MAX_PAGES_LIMIT = 100;

    public void search(SearchDto searchDto) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            log.info("Starting background ingestion pipeline for: {}", searchDto);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ObjectNode requestNode = objectMapper.createObjectNode();
            if (searchDto.getTitle() != null) requestNode.put("title", searchDto.getTitle());
            if (searchDto.getDateFrom() != null) requestNode.put("dateFrom", searchDto.getDateFrom().format(dateFormatter));
            if (searchDto.getDateTo() != null) requestNode.put("dateTo", searchDto.getDateTo().format(dateFormatter));

            if (searchDto.getCpvItems() != null && !searchDto.getCpvItems().isEmpty()) {
                ArrayNode cpvArray = requestNode.putArray("cpvItems");
                searchDto.getCpvItems().forEach(cpvArray::add);
            }

            String requestBody = objectMapper.writeValueAsString(requestNode);
            int page = 0;
            boolean lastPage = false;
            int totalNewRecords = 0;

            // Pagination loop: Continues until 'last: true' or MAX_PAGES_LIMIT is reached.
            while (!lastPage && page < MAX_PAGES_LIMIT) {
                String url = BASE_URL + page;
                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    JsonNode content = responseJson.path("content");

                    List<Bid> batchToSave = new ArrayList<>();
                    for (JsonNode bidNode : content) {
                        Bid bid = mapJsonToBid(bidNode);
                        // Prevent duplicates based on unique Reference Number
                        if (bidRepository.findByReferenceNumber(bid.getReferenceNumber()).isEmpty()) {
                            batchToSave.add(bid);
                        }
                    }

                    if (!batchToSave.isEmpty()) {
                        bidRepository.saveAll(batchToSave);
                        totalNewRecords += batchToSave.size();
                    }

                    lastPage = responseJson.path("last").asBoolean(true);
                    log.info("Page {}: Ingested {} records. Last page: {}", page, batchToSave.size(), lastPage);
                    page++;
                } else {
                    log.warn("API responded with error at page {}. Stopping ingestion.", page);
                    break;
                }
            }
            log.info("Ingestion completed successfully. Total new records: {}", totalNewRecords);
        } catch (Exception e) {
            log.error("CRITICAL: Ingestion pipeline failed", e);
        }
    }

    /**
     * Maps raw JSON to Bid entity with deep-path extraction for CPV codes.
     * Optimized based on logs for reference numbers like 25PROC018268798.
     */
    private Bid mapJsonToBid(JsonNode node) {
        String referenceNumber = node.path("referenceNumber").asText(null);
        String title = node.path("title").asText("N/A");
        String organization = node.path("organization").path("value").asText("Unknown");

        // 1. Date and Cost
        String dateStr = node.path("publishDate").asText("");
        if (dateStr.isEmpty()) dateStr = node.path("signedDate").asText("");
        LocalDate date = (!dateStr.isEmpty()) ? LocalDate.parse(dateStr.substring(0, 10)) : LocalDate.now();


        double extractedCost = node.path("totalCostWithVAT").asDouble(0.0);
        if (extractedCost == 0.0) {
            extractedCost = node.path("budgetAmountVAT").asDouble(0.0);
        }

        // 2. NUTS Extraction
        String extractedNuts = "N/A";
        if (node.has("nutsCode")) {
            JsonNode nNode = node.path("nutsCode");
            extractedNuts = nNode.has("key") ? nNode.path("key").asText() : nNode.asText();
        }

        // 3. DEEP CPV EXTRACTION
        String extractedCpv = "N/A";

        // Path A: New path (objectDetails -> cpvs -> key)
        if (node.has("objectDetails") && node.path("objectDetails").isArray() && !node.path("objectDetails").isEmpty()) {
            JsonNode firstDetail = node.path("objectDetails").get(0);
            if (firstDetail.has("cpvs") && firstDetail.path("cpvs").isArray() && !firstDetail.path("cpvs").isEmpty()) {
                extractedCpv = firstDetail.path("cpvs").get(0).path("key").asText("N/A");
            }
        }

        // Path B: Fallback in cpvItems (if Path A fails)
        if ("N/A".equals(extractedCpv) && node.has("cpvItems") && node.path("cpvItems").isArray() && !node.path("cpvItems").isEmpty()) {
            JsonNode first = node.path("cpvItems").get(0);
            extractedCpv = first.has("key") ? first.path("key").asText() : first.asText();
        }

        // Path C: Fallback σε cpvCode ή mainCpvCode
        if ("N/A".equals(extractedCpv)) {
            if (node.has("cpvCode")) {
                extractedCpv = node.path("cpvCode").has("key") ? node.path("cpvCode").path("key").asText() : node.path("cpvCode").asText();
            } else if (node.has("mainCpvCode")) {
                extractedCpv = node.path("mainCpvCode").has("key") ? node.path("mainCpvCode").path("key").asText() : node.path("mainCpvCode").asText();
            }
        }

        //  Clean
        if (extractedCpv != null && !extractedCpv.equals("N/A")) {
            extractedCpv = extractedCpv.split("-")[0].trim();
        } else {
            // Αν ακόμα είναι N/A, το log θα μας πει γιατί
            log.warn("CPV STILL MISSING for {}. Path verified?", referenceNumber);
        }

        return new Bid(referenceNumber, title, "AI-Ingest", extractedCpv, organization, date, extractedCost, extractedNuts);
    }

    public List<Bid> findAllStoredBids() {
        return bidRepository.findAll();
    }
}