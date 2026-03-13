package com.pricecompare.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Response for the AI-powered image search endpoint.
 * Contains Gemini's product identification + live scraped results per store.
 */
@Data
@Builder
public class AiSearchResponse {

    /** Product name identified by Gemini Vision */
    private String identifiedProduct;

    /** Search query actually used (may be cleaned up from identifiedProduct) */
    private String searchQuery;

    /** Total number of results across all stores */
    private int totalResults;

    /** Results grouped by store name */
    private Map<String, List<ScrapedProductResult>> resultsByStore;

    /** Flat list of all results sorted by price */
    private List<ScrapedProductResult> allResults;
}
