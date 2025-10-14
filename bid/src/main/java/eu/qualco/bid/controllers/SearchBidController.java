package eu.qualco.bid.controllers;

import eu.qualco.bid.dtos.SearchDto;
import eu.qualco.bid.services.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/search-bid")
@Tag(name = "Search Bids", description = "Endpoints for searching bids based on various criteria")
@Slf4j
@RequiredArgsConstructor
public class SearchBidController {

    private final SearchService searchService;

    @PostMapping
    @Operation(
            summary = "Search bids",
            description = "Search for bids using various filters such as title, CPV codes, organizations, dates, and cost range."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search results returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> searchBids(
            @RequestBody(description = "Search criteria for filtering bids", required = true)
            @org.springframework.web.bind.annotation.RequestBody SearchDto searchDto
    ) {
        log.info("searchBids with data: {}", searchDto);
        // Call the service to fetch data from the web service using searchDto
        searchService.search(searchDto);
        return ResponseEntity.ok(searchDto);
    }
}
