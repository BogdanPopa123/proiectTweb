package com.pricecompare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * Calls Google Gemini Vision API to identify a product from an uploaded image.
 * Returns a concise product name suitable for use as a search query.
 */
@Service
@Slf4j
public class GeminiService {

    private static final String PROMPT =
            "Identify the exact product in this image. " +
            "Reply with ONLY the product name — no explanations, no punctuation, no extra words. " +
            "Be as specific as possible: include brand, model name, model number, generation, and color if visible. " +
            "Good examples: " +
            "Apple iPhone 17 Pro Max Black Titanium, " +
            "Samsung Galaxy S25 Ultra 256GB Titanium Silver, " +
            "Apple MacBook Pro 14 M4 Pro Space Black, " +
            "Sony WH-1000XM5 Black, " +
            "Apple Watch Series 10 46mm Black. " +
            "If you cannot identify it, reply: unknown product";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String baseUrl;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends the image to Gemini Vision and returns the identified product name.
     */
    public String identifyProduct(byte[] imageBytes, String mimeType) throws Exception {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key is not configured. Set GEMINI_API_KEY in your .env file.");
        }

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String requestBody = buildRequestBody(base64Image, mimeType);

        String url = baseUrl + "/" + model + ":generateContent?key=" + apiKey;

        log.debug("Calling Gemini API: model={}, imageSize={}B, mimeType={}", model, imageBytes.length, mimeType);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            log.warn("Gemini API rate limit hit");
            throw new RuntimeException("Gemini API quota exceeded. Please wait a few minutes and try again, or enable billing at console.cloud.google.com.");
        }
        if (response.statusCode() == 400) {
            log.error("Gemini API bad request: {}", response.body());
            throw new RuntimeException("Gemini API rejected the request. The image may be too large or in an unsupported format.");
        }
        if (response.statusCode() != 200) {
            log.error("Gemini API error: status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("Gemini API error (status " + response.statusCode() + "). Please try again later.");
        }

        String identified = parseResponse(response.body());
        log.info("Gemini identified product: '{}'", identified);
        return identified;
    }

    private String buildRequestBody(String base64Image, String mimeType) throws Exception {
        // Build the Gemini API request JSON manually to avoid pulling in extra deps
        var body = objectMapper.createObjectNode();
        var contents = objectMapper.createArrayNode();
        var content = objectMapper.createObjectNode();
        var parts = objectMapper.createArrayNode();

        // Image part
        var imagePart = objectMapper.createObjectNode();
        var inlineData = objectMapper.createObjectNode();
        inlineData.put("mime_type", mimeType != null ? mimeType : "image/jpeg");
        inlineData.put("data", base64Image);
        imagePart.set("inline_data", inlineData);

        // Text part (prompt)
        var textPart = objectMapper.createObjectNode();
        textPart.put("text", PROMPT);

        parts.add(imagePart);
        parts.add(textPart);
        content.set("parts", parts);
        contents.add(content);
        body.set("contents", contents);

        // Generation config
        var generationConfig = objectMapper.createObjectNode();
        generationConfig.put("maxOutputTokens", 300);
        generationConfig.put("temperature", 0.1);

        // Disable thinking — not needed for simple product identification
        // and causes over-analysis leading to "unknown product" on unofficial images
        var thinkingConfig = objectMapper.createObjectNode();
        thinkingConfig.put("thinkingBudget", 0);
        generationConfig.set("thinkingConfig", thinkingConfig);

        body.set("generationConfig", generationConfig);

        return objectMapper.writeValueAsString(body);
    }

    private String parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        log.info("Gemini raw response: {}", responseBody);

        // Check for API-level error
        JsonNode error = root.path("error");
        if (!error.isMissingNode()) {
            String msg = error.path("message").asText("Unknown Gemini error");
            throw new RuntimeException("Gemini API error: " + msg);
        }

        JsonNode candidates = root.path("candidates");
        if (candidates.isMissingNode() || candidates.isEmpty()) {
            log.warn("Gemini returned no candidates. Full response: {}", responseBody);
            return "unknown product";
        }

        JsonNode firstCandidate = candidates.get(0);
        if (firstCandidate == null) {
            log.warn("Gemini candidates array is empty");
            return "unknown product";
        }

        // Check finish reason — SAFETY means the image was blocked
        String finishReason = firstCandidate.path("finishReason").asText("");
        if ("SAFETY".equals(finishReason) || "RECITATION".equals(finishReason)) {
            log.warn("Gemini blocked response: finishReason={}", finishReason);
            return "unknown product";
        }

        JsonNode parts = firstCandidate.path("content").path("parts");
        if (parts.isMissingNode() || parts.isEmpty() || parts.get(0) == null) {
            log.warn("Gemini response has no parts. Candidate: {}", firstCandidate);
            return "unknown product";
        }

        String text = parts.get(0).path("text").asText("").trim();
        text = text.replaceAll("\\s+", " ").trim();

        log.info("Gemini identified: '{}'", text);
        return text.isEmpty() ? "unknown product" : text;
    }
}
