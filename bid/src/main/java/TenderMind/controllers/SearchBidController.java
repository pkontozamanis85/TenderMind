package TenderMind.controllers;

import TenderMind.dtos.SearchDto;
import TenderMind.model.Bid;
import TenderMind.services.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller handling the ingestion of external tender data.
 * Acts as the entry point for synchronizing the local database with government open data.
 */
@Slf4j
@RestController
@RequestMapping("/api/search-bid")
@Tag(name = "Search & Ingestion", description = "Endpoints for triggering and retrieving tender notices")
@RequiredArgsConstructor
public class SearchBidController {

    private final SearchService searchService;

    /**
     * Triggers a paginated search against the National eProcurement API.
     * Persists new bids while skipping duplicates.
     */
    @PostMapping
    @Operation(summary = "Ingest bids from external API based on filters")
    public ResponseEntity<List<Bid>> searchBids(@RequestBody SearchDto searchDto) {
        log.info("Request received to fetch bids from external source: {}", searchDto);
        List<Bid> newlyIngestedBids = searchService.search(searchDto);
        log.info("Batch ingestion completed. {} new records saved.", newlyIngestedBids.size());
        return ResponseEntity.ok(newlyIngestedBids);
    }

    /**
     * Returns all bids stored in the local H2 database.
     * Crucial for the Python ML Engine to retrieve data for similarity analysis.
     */
    @GetMapping("/all")
    @Operation(summary = "Retrieve all cached bids for the matching engine")
    public ResponseEntity<List<Bid>> getAllBids() {
        List<Bid> bids = searchService.findAllStoredBids();
        log.info("Matching engine requested bid data. Returning {} records.", bids.size());
        return ResponseEntity.ok(bids);
    }
}