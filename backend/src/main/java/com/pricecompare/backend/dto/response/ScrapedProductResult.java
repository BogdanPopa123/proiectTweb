package com.pricecompare.backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Represents a single product scraped on-demand from a store's search results.
 * Not persisted to the database — used only in AI search responses.
 */
@Data
@Builder
public class ScrapedProductResult {
    private String name;
    private String productUrl;
    private String imageUrl;
    private String price;
    private String currency;
    private String storeName;
    private String storeUrl;
}
