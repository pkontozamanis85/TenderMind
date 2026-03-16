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
import java.util.stream.StreamSupport;

/**
 * Service Layer responsible for data acquisition from the Greek eProcurement Open Data API.
 * Handles paginated ingestion, JSON mapping, and duplicate prevention.
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BidRepository bidRepository;

    private static final String BASE_URL = "https://cerpp.eprocurement.gov.gr/khmdhs-opendata/notice?page=";

    /**
     * Safety limit to prevent infinite loops and timeouts during bulk ingestion.
     * Higher values will fetch more data but might trigger client timeouts.
     */
    private static final int MAX_PAGES_LIMIT = 10;

    /**
     * Executes a paginated search against the government API and persists new bids.
     * Uses effectively final variables to comply with Java Lambda requirements.
     */
    public List<Bid> search(SearchDto searchDto) {
        List<Bid> allSavedBids = new ArrayList<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        try {
            log.info("Starting ingestion for criteria: {}", searchDto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            // Build dynamic request payload
            ObjectNode requestNode = objectMapper.createObjectNode();
            if (searchDto.getTitle() != null) requestNode.put("title", searchDto.getTitle());
            if (searchDto.getDateFrom() != null) requestNode.put("dateFrom", searchDto.getDateFrom().format(dateFormatter));
            if (searchDto.getDateTo() != null) requestNode.put("dateTo", searchDto.getDateTo().format(dateFormatter));

            // Map CPV Items array
            if (searchDto.getCpvItems() != null && !searchDto.getCpvItems().isEmpty()) {
                ArrayNode cpvArray = requestNode.putArray("cpvItems");
                searchDto.getCpvItems().forEach(cpvArray::add);
            }

            String requestBody = objectMapper.writeValueAsString(requestNode);
            int page = 0;
            boolean lastPage = false;

            while (!lastPage && page < MAX_PAGES_LIMIT) {
                // Create a final copy of the current page index
                final int currentPageForLambda = page;
                log.info("Fetching Page {}/{}...", currentPageForLambda, MAX_PAGES_LIMIT - 1);

                String url = BASE_URL + currentPageForLambda;
                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    JsonNode content = responseJson.path("content");

                    List<Bid> batchToSave = new ArrayList<>();
                    StreamSupport.stream(content.spliterator(), false).forEach(bidNode -> {
                        try {
                            Bid bid = mapJsonToBid(bidNode);

                            // Idempotency check: Skip if reference number exists
                            if (bidRepository.findByReferenceNumber(bid.getReferenceNumber()).isEmpty()) {
                                batchToSave.add(bid);
                            }
                        } catch (Exception e) {
                            log.warn("Mapping error on page {}: {}", currentPageForLambda, e.getMessage());
                        }
                    });

                    if (!batchToSave.isEmpty()) {
                        List<Bid> saved = bidRepository.saveAll(batchToSave);
                        allSavedBids.addAll(saved);
                        log.info("Saved {} new bids from page {}.", saved.size(), currentPageForLambda);
                    }

                    lastPage = responseJson.path("last").asBoolean(false);
                    page++; // Normal increment for the while loop
                } else {
                    log.error("External API error on page {}: {}", currentPageForLambda, response.getStatusCode());
                    break;
                }
            }
            log.info("Ingestion completed successfully. Total records stored: {}", allSavedBids.size());

        } catch (Exception e) {
            log.error("Fatal failure during bid ingestion pipeline", e);
        }
        return allSavedBids;
    }

    /**
     * Retrieves all cached tenders from H2 for the Python ML Engine.
     */
    public List<Bid> findAllStoredBids() {
        return bidRepository.findAll();
    }

    /**
     * Maps raw JSON node from the Greek API to the internal Bid domain model.
     */
    private Bid mapJsonToBid(JsonNode node) {
        String referenceNumber = node.path("referenceNumber").asText(null);
        String title = node.path("title").asText("No Title Provided");

        // Complex object mapping (NUTS & Organization)
        String nutsCode = node.path("nutsCode").path("key").asText("N/A");
        String organization = node.path("organization").path("value").asText("Unknown Organization");

        // Date handling with fallback
        String dateStr = node.path("publishDate").asText("");
        if (dateStr.isEmpty()) dateStr = node.path("signedDate").asText("");
        LocalDate date = (!dateStr.isEmpty()) ? LocalDate.parse(dateStr.substring(0, 10)) : LocalDate.now();

        // Financial data mapping
        Double cost = node.path("totalCostWithVAT").isNumber() ? node.path("totalCostWithVAT").asDouble() : 0.0;

        // Industry classification
        String cpv = node.path("cpvCode").path("key").asText("N/A");

        return new Bid(referenceNumber, title, "Automated AI Ingestion", cpv, organization, date, cost, nutsCode);
    }
}