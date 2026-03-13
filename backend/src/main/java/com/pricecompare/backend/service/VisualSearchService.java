package com.pricecompare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pricecompare.backend.dto.response.ProductResponse;
import com.pricecompare.backend.dto.response.VisualSearchResponse;
import com.pricecompare.backend.entity.Product;
import com.pricecompare.backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class VisualSearchService {

    private static final int PHASH_THRESHOLD = 10;

    private final ProductRepository productRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String imageServiceUrl;

    public VisualSearchService(ProductRepository productRepository,
                               @Value("${image.service.url:http://localhost:5001}") String imageServiceUrl) {
        this.productRepository = productRepository;
        this.imageServiceUrl = imageServiceUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Calls the Python image-service to compute the DCT pHash for an image.
     * Supports all formats Pillow handles: JPEG, PNG, WebP, BMP, GIF, TIFF, HEIC, etc.
     */
    public String computePhash(byte[] imageBytes) throws IOException {
        return computePhash(imageBytes, "image.jpg");
    }

    public String computePhash(byte[] imageBytes, String filename) throws IOException {
        // Build multipart/form-data body manually
        String boundary = UUID.randomUUID().toString();
        byte[] multipartBody = buildMultipart(boundary, imageBytes, filename);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(imageServiceUrl + "/phash"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                String hash = json.get("hash").asText();
                log.debug("pHash from image-service: {}", hash);
                return hash;
            } else {
                // Parse error detail from Python response
                JsonNode json = objectMapper.readTree(response.body());
                String detail = json.has("detail") ? json.get("detail").asText() : response.body();
                throw new IOException(detail);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Image service request interrupted", e);
        }
    }

    /**
     * Fetches an image from a URL and computes its pHash via the image-service.
     * Used by scrapers to hash product images.
     */
    public String computePhashFromUrl(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .header("User-Agent", "PriceCompare/1.0")
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

            if (response.statusCode() == 200) {
                String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
                return computePhash(response.body(), filename);
            }
        } catch (Exception e) {
            log.debug("Failed to compute pHash for URL {}: {}", imageUrl, e.getMessage());
        }
        return null;
    }

    /**
     * Computes Hamming distance between two hex-encoded hashes.
     */
    public int hammingDistance(String hash1, String hash2) {
        if (hash1 == null || hash2 == null) return Integer.MAX_VALUE;
        try {
            long h1 = Long.parseUnsignedLong(hash1, 16);
            long h2 = Long.parseUnsignedLong(hash2, 16);
            return Long.bitCount(h1 ^ h2);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Finds products visually similar to the uploaded image.
     */
    public VisualSearchResponse findSimilarProducts(byte[] imageBytes, String filename) throws IOException {
        String queryHash = computePhash(imageBytes, filename);
        List<Product> allProducts = productRepository.findAllWithPhash();

        List<VisualSearchResponse.SimilarProductResult> results = new ArrayList<>();

        for (Product product : allProducts) {
            int distance = hammingDistance(queryHash, product.getPhash());
            if (distance <= PHASH_THRESHOLD) {
                double similarity = 1.0 - ((double) distance / 64.0);
                results.add(VisualSearchResponse.SimilarProductResult.builder()
                        .product(ProductResponse.from(product))
                        .hammingDistance(distance)
                        .similarityScore(Math.round(similarity * 1000.0) / 1000.0)
                        .build());
            }
        }

        results.sort(Comparator.comparingInt(VisualSearchResponse.SimilarProductResult::getHammingDistance));

        return VisualSearchResponse.builder()
                .totalResults(results.size())
                .results(results)
                .build();
    }

    // Keep for backward compatibility
    public VisualSearchResponse findSimilarProducts(byte[] imageBytes) throws IOException {
        return findSimilarProducts(imageBytes, "image.jpg");
    }

    /**
     * Builds a multipart/form-data body for the image upload to the Python service.
     */
    private byte[] buildMultipart(String boundary, byte[] imageBytes, String filename) throws IOException {
        String contentType = guessContentType(filename);
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"image\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";

        byte[] headerBytes = header.getBytes();
        byte[] footerBytes = footer.getBytes();
        byte[] body = new byte[headerBytes.length + imageBytes.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(imageBytes, 0, body, headerBytes.length, imageBytes.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + imageBytes.length, footerBytes.length);
        return body;
    }

    private String guessContentType(String filename) {
        if (filename == null) return "image/jpeg";
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".bmp")) return "image/bmp";
        if (lower.endsWith(".tiff") || lower.endsWith(".tif")) return "image/tiff";
        return "image/jpeg";
    }
}
