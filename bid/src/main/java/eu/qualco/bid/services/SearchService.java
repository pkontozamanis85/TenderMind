package eu.qualco.bid.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.qualco.bid.dtos.SearchDto;
import eu.qualco.bid.model.Bid;
import eu.qualco.bid.repositories.BidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@Service
public class SearchService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final BidRepository bidRepository;

    private static final String BASE_URL = "https://cerpp.eprocurement.gov.gr/khmdhs-opendata/notice?page=";

    public void search(SearchDto searchDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            ObjectNode requestNode = objectMapper.createObjectNode();

            if (searchDto.getTitle() != null) requestNode.put("title", searchDto.getTitle());
            if (searchDto.getCpvItems() != null) requestNode.putPOJO("cpvItems", searchDto.getCpvItems());
            if (searchDto.getOrganizations() != null) requestNode.putPOJO("organizations", searchDto.getOrganizations());
            if (searchDto.getSigner() != null) requestNode.put("signer", searchDto.getSigner());
            if (searchDto.getContractType() != null) requestNode.put("contractType", searchDto.getContractType());
            if (searchDto.getDateFrom() != null) requestNode.put("dateFrom", searchDto.getDateFrom().format(dateFormatter));
            if (searchDto.getDateTo() != null) requestNode.put("dateTo", searchDto.getDateTo().format(dateFormatter));
            if (searchDto.getTotalCostFrom() != null) requestNode.put("totalCostFrom", searchDto.getTotalCostFrom());
            if (searchDto.getTotalCostTo() != null) requestNode.put("totalCostTo", searchDto.getTotalCostTo());
            if (searchDto.getCancelDateFrom() != null) requestNode.put("cancelDateFrom", searchDto.getCancelDateFrom().format(dateFormatter));
            if (searchDto.getCancelDateTo() != null) requestNode.put("cancelDateTo", searchDto.getCancelDateTo().format(dateFormatter));
            if (searchDto.getReferenceNumber() != null) requestNode.put("referenceNumber", searchDto.getReferenceNumber());
            if (searchDto.getProcedureType() != null) requestNode.put("procedureType", searchDto.getProcedureType());
            if (searchDto.getFinalDateFrom() != null) requestNode.put("finalDateFrom", searchDto.getFinalDateFrom().format(dateTimeFormatter));
            if (searchDto.getFinalDateTo() != null) requestNode.put("finalDateTo", searchDto.getFinalDateTo().format(dateTimeFormatter));
            if (searchDto.getAaht() != null) requestNode.put("aaht", searchDto.getAaht());

            String requestBody = objectMapper.writeValueAsString(requestNode);

            int page = 0;
            boolean lastPage = false;
            ArrayNode allResults = objectMapper.createArrayNode();

            while (!lastPage) {
                String url = BASE_URL + page;
                HttpEntity<String> request = new HttpEntity<>(requestBody, headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    JsonNode content = responseJson.path("content");
                    if (content.isArray()) {
                        allResults.addAll((ArrayNode) content);
                    }
                    StreamSupport.stream(content.spliterator(), true).forEach(bidNode -> {
                        try {
                            Bid bid = mapJsonToBid(bidNode);
                            LocalDate dateFrom = searchDto.getDateFrom();
                            LocalDate dateTo = searchDto.getDateTo();

                            if (bid.getDate() != null) {
                                if ((dateFrom != null && bid.getDate().isBefore(dateFrom)) ||
                                        (dateTo != null && bid.getDate().isAfter(dateTo))) {
                                    log.debug("Skipping bid outside date range: {} (date: {})", bid.getReferenceNumber(), bid.getDate());
                                    return;
                                }
                            }

                            if (bidRepository.findByReferenceNumber(bid.getReferenceNumber()).isPresent()) {
                                log.debug("Skipping duplicate bid: {}", bid.getReferenceNumber());
                                return;
                            }

                            bidRepository.save(bid);
                            log.debug("Saved new bid with referenceNumber: {}", bid.getReferenceNumber());
                        } catch (Exception e) {
                            log.warn("Failed to map or save bid: {}", e.getMessage());
                        }
                    });


                    lastPage = responseJson.path("last").asBoolean(false);
                    page++;
                } else {
                    log.error("Failed at page {}: {}", page, response.getStatusCode());
                    break;
                }
            }

        } catch (Exception e) {
            log.error("Exception during paginated search", e);
            objectMapper.createObjectNode().put("error", e.getMessage());
        }
    }


    private Bid mapJsonToBid(JsonNode node) {
        String referenceNumber = node.path("referenceNumber").asText(null);
        String title = node.path("title").asText(null);
        String nutsCode = node.path("nutsCode").path("key").asText(null);
        String description = null;
        String cpvCode = null;

        JsonNode objectDetails = node.path("objectDetails");
        if (objectDetails.isArray() && !objectDetails.isEmpty()) {
            JsonNode firstDetail = objectDetails.get(0);

            // Extract shortDescription
            description = firstDetail.path("shortDescription").asText(null);

            // Extract cpvCode
            JsonNode cpvs = firstDetail.path("cpvs");
            if (cpvs.isArray() && !cpvs.isEmpty()) {
                cpvCode = cpvs.get(0).path("key").asText(null);
            }
        }


        // Extract organization name
        String organization = node.path("organization").path("value").asText(null);

        // Extract date from signedDate
        String dateStr = node.path("signedDate").asText(null);
        LocalDate date = null;
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr.substring(0, 10));

            } catch (Exception e) {
                log.warn("Failed to parse signedDate: {}", dateStr);
            }
        }


        // Extract cost from totalCostWithVAT
        Double cost = node.has("totalCostWithVAT") && !node.get("totalCostWithVAT").isNull()
                ? node.get("totalCostWithVAT").asDouble()
                : null;

        return new Bid(referenceNumber, title, description, cpvCode, organization, date, cost, nutsCode);

    }



}
