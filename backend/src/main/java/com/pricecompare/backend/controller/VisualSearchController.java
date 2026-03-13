package com.pricecompare.backend.controller;

import com.pricecompare.backend.dto.response.AiSearchResponse;
import com.pricecompare.backend.dto.response.ScrapedProductResult;
import com.pricecompare.backend.dto.response.VisualSearchResponse;
import com.pricecompare.backend.exception.BadRequestException;
import com.pricecompare.backend.service.GeminiService;
import com.pricecompare.backend.service.OnDemandScraperService;
import com.pricecompare.backend.service.VisualSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/visual-search")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Visual Search", description = "Upload an image to find visually similar products")
public class VisualSearchController {

    private final VisualSearchService visualSearchService;
    private final GeminiService geminiService;
    private final OnDemandScraperService onDemandScraperService;

    // ──────────────────────────────────────────────────────────────
    //  Existing: pHash-based search against the local DB
    // ──────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Find visually similar products using pHash (searches local DB)")
    public ResponseEntity<VisualSearchResponse> searchByImage(
            @RequestPart("image") MultipartFile image) {

        log.info("pHash search: filename={}, contentType={}, size={}B",
                image.getOriginalFilename(), image.getContentType(), image.getSize());

        validateImage(image);

        try {
            byte[] imageBytes = image.getBytes();
            String filename = image.getOriginalFilename() != null ? image.getOriginalFilename() : "image.jpg";
            VisualSearchResponse response = visualSearchService.findSimilarProducts(imageBytes, filename);
            log.info("pHash search complete: {} results", response.getTotalResults());
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to process image for pHash search: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to process image: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during pHash search: filename={}", image.getOriginalFilename(), e);
            throw new BadRequestException("Unexpected error processing image: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  New: AI-powered search — Gemini identifies product → live scrape
    // ──────────────────────────────────────────────────────────────

    @PostMapping(value = "/ai-search", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "AI-powered search: Gemini identifies the product then scrapes all stores live")
    public ResponseEntity<AiSearchResponse> aiSearchByImage(
            @RequestPart("image") MultipartFile image) {

        log.info("AI search: filename={}, contentType={}, size={}B",
                image.getOriginalFilename(), image.getContentType(), image.getSize());

        validateImage(image);

        if (!geminiService.isConfigured()) {
            throw new BadRequestException("AI search is not available: GEMINI_API_KEY is not configured.");
        }

        try {
            byte[] imageBytes = image.getBytes();
            String mimeType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            // Step 1: Ask Gemini to identify the product
            String identified = geminiService.identifyProduct(imageBytes, mimeType);
            log.info("Gemini identified: '{}'", identified);

            if ("unknown product".equalsIgnoreCase(identified)) {
                return ResponseEntity.ok(AiSearchResponse.builder()
                        .identifiedProduct(identified)
                        .searchQuery(identified)
                        .totalResults(0)
                        .resultsByStore(Map.of())
                        .allResults(List.of())
                        .build());
            }

            // Step 2: Use identified name as search query on all stores
            String searchQuery = identified;
            List<ScrapedProductResult> allResults = onDemandScraperService.searchAllStores(searchQuery);

            // Group by store
            Map<String, List<ScrapedProductResult>> byStore = allResults.stream()
                    .collect(Collectors.groupingBy(ScrapedProductResult::getStoreName));

            // Sort flat list: results with price first, then alphabetically by store
            List<ScrapedProductResult> sorted = allResults.stream()
                    .sorted(Comparator.comparing(r -> r.getStoreName()))
                    .collect(Collectors.toList());

            log.info("AI search complete: identified='{}', totalResults={}", identified, allResults.size());

            return ResponseEntity.ok(AiSearchResponse.builder()
                    .identifiedProduct(identified)
                    .searchQuery(searchQuery)
                    .totalResults(allResults.size())
                    .resultsByStore(byStore)
                    .allResults(sorted)
                    .build());

        } catch (IOException e) {
            log.error("Failed to read image for AI search: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to process image: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during AI search: filename={}", image.getOriginalFilename(), e);
            throw new BadRequestException("AI search failed: " + e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────────────
    //  Helpers
    // ──────────────────────────────────────────────────────────────

    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BadRequestException("File must be an image (JPEG, PNG, WebP, etc.)");
        }
    }
}
