package TenderMind.controllers;

import TenderMind.dtos.SearchDto;
import TenderMind.services.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/search-bid")
@Tag(name = "Search & Ingestion", description = "Endpoints for triggering and retrieving tender notices")
@RequiredArgsConstructor
public class SearchBidController {

    private final SearchService searchService;

    /**
     * Triggers a paginated search asynchronously.
     * Returns 202 Accepted immediately to prevent client-side ReadTimeout.
     */
    @PostMapping
    @Operation(summary = "Ingest bids from external API (Async)")
    public ResponseEntity<String> searchBids(@RequestBody SearchDto searchDto) {
        log.info("Async request received for ingestion: {}", searchDto);

        // Fire and forget: The service handles the persistence in the background
        CompletableFuture.runAsync(() -> searchService.search(searchDto))
                .exceptionally(ex -> {
                    log.error("Background ingestion failed: {}", ex.getMessage());
                    return null;
                });

        return ResponseEntity.accepted().body("Ingestion process started in the background.");
    }

    @GetMapping("/all")
    @Operation(summary = "Retrieve all cached bids for the matching engine")
    public ResponseEntity<?> getAllBids() {
        return ResponseEntity.ok(searchService.findAllStoredBids());
    }
}