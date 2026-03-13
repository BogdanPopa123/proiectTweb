package com.pricecompare.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VisualSearchResponse {
    private int totalResults;
    private List<SimilarProductResult> results;

    @Data
    @Builder
    public static class SimilarProductResult {
        private ProductResponse product;
        private int hammingDistance;
        private double similarityScore;
    }
}
