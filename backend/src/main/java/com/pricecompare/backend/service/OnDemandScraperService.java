package com.pricecompare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricecompare.backend.dto.response.ScrapedProductResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Delegates on-demand product searches to the Python image-service,
 * which uses Playwright (headless Chromium) to handle JS-rendered pages.
 * All three stores are searched in parallel for speed.
 */
@Service
@Slf4j
public class OnDemandScraperService {

    private static final int MAX_RESULTS_PER_STORE = 6;
    private static final List<String> STORES = List.of("emag", "altex", "cel");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String imageServiceUrl;
    private final ExecutorService executor;

    public OnDemandScraperService(
            @Value("${image.service.url:http://localhost:5001}") String imageServiceUrl) {
        this.imageServiceUrl = imageServiceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        // One thread per store so all three searches run in parallel
        this.executor = Executors.newFixedThreadPool(3);
    }

    /**
     * Searches all stores in parallel and returns combined results.
     */
    public List<ScrapedProductResult> searchAllStores(String query) {
        log.info("Starting parallel search for '{}' across {} stores", query, STORES);

        List<CompletableFuture<List<ScrapedProductResult>>> futures = STORES.stream()
                .map(store -> CompletableFuture.supplyAsync(
                        () -> searchStore(store, query), executor))
                .toList();

        List<ScrapedProductResult> combined = new ArrayList<>();
        for (CompletableFuture<List<ScrapedProductResult>> future : futures) {
            try {
                combined.addAll(future.get());
            } catch (Exception e) {
                log.warn("A store search failed: {}", e.getMessage());
            }
        }

        log.info("Combined results: {} products for '{}'", combined.size(), query);
        return combined;
    }

    /**
     * Calls the Python image-service /search endpoint for a single store.
     */
    public List<ScrapedProductResult> searchStore(String store, String query) {
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = imageServiceUrl + "/search?store=" + store
                    + "&query=" + encoded
                    + "&limit=" + MAX_RESULTS_PER_STORE;

            log.info("[{}] Calling image-service: {}", store, url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60)) // Playwright needs more time
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseResults(response.body(), store);
            } else {
                log.warn("[{}] image-service returned {}: {}", store, response.statusCode(), response.body());
                return List.of();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("[{}] Search interrupted", store);
            return List.of();
        } catch (Exception e) {
            log.warn("[{}] Search failed: {}", store, e.getMessage());
            return List.of();
        }
    }

    private List<ScrapedProductResult> parseResults(String json, String store) {
        List<ScrapedProductResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode items = root.get("results");
            if (items == null || !items.isArray()) return results;

            for (JsonNode item : items) {
                String name       = text(item, "name");
                String productUrl = text(item, "productUrl");
                if (name == null || productUrl == null) continue;

                results.add(ScrapedProductResult.builder()
                        .name(name)
                        .productUrl(productUrl)
                        .imageUrl(text(item, "imageUrl"))
                        .price(text(item, "price"))
                        .currency("RON")
                        .storeName(text(item, "storeName"))
                        .storeUrl(text(item, "storeUrl"))
                        .build());
            }

            log.info("[{}] Parsed {} results", store, results.size());
        } catch (Exception e) {
            log.error("[{}] Failed to parse response: {}", store, e.getMessage());
        }
        return results;
    }

    private String text(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }
}
