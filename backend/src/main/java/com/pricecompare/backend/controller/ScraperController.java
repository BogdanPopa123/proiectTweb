package com.pricecompare.backend.controller;

import com.pricecompare.backend.service.scraper.AltexScraperService;
import com.pricecompare.backend.service.scraper.CelScraperService;
import com.pricecompare.backend.service.scraper.EmagScraperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/scrapers")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Scrapers", description = "Manually trigger product scrapers (Admin only)")
public class ScraperController {

    private final EmagScraperService emagScraperService;
    private final AltexScraperService altexScraperService;
    private final CelScraperService celScraperService;

    @PostMapping("/run-all")
    @Operation(summary = "Trigger all scrapers immediately", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> runAll() {
        log.info("Manual scrape triggered: all scrapers");
        // Run in a background thread so the HTTP response returns immediately
        Thread.ofVirtual().start(() -> {
            try { emagScraperService.scrape(); } catch (Exception e) { log.error("Emag scraper failed", e); }
            try { altexScraperService.scrape(); } catch (Exception e) { log.error("Altex scraper failed", e); }
            try { celScraperService.scrape(); } catch (Exception e) { log.error("Cel scraper failed", e); }
        });
        return ResponseEntity.ok(Map.of("status", "started", "message",
                "All scrapers started in background. Check logs for progress."));
    }

    @PostMapping("/run")
    @Operation(summary = "Trigger a specific scraper", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Map<String, String>> runOne(@RequestParam String store) {
        log.info("Manual scrape triggered: {}", store);
        switch (store.toLowerCase()) {
            case "emag" -> Thread.ofVirtual().start(() -> {
                try { emagScraperService.scrape(); } catch (Exception e) { log.error("Emag scraper failed", e); }
            });
            case "altex" -> Thread.ofVirtual().start(() -> {
                try { altexScraperService.scrape(); } catch (Exception e) { log.error("Altex scraper failed", e); }
            });
            case "cel" -> Thread.ofVirtual().start(() -> {
                try { celScraperService.scrape(); } catch (Exception e) { log.error("Cel scraper failed", e); }
            });
            default -> { return ResponseEntity.badRequest().body(Map.of("error", "Unknown store. Use: emag, altex, cel")); }
        }
        return ResponseEntity.ok(Map.of("status", "started", "message",
                store + " scraper started in background. Check logs for progress."));
    }
}
