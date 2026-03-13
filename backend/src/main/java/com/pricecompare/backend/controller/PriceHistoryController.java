package com.pricecompare.backend.controller;

import com.pricecompare.backend.dto.response.PriceHistoryResponse;
import com.pricecompare.backend.service.PriceHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/price-history")
@RequiredArgsConstructor
@Tag(name = "Price History", description = "Product price history")
public class PriceHistoryController {

    private final PriceHistoryService priceHistoryService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get price history for a product")
    public ResponseEntity<List<PriceHistoryResponse>> getHistoryForProduct(
            @PathVariable Long productId) {
        return ResponseEntity.ok(priceHistoryService.getHistoryForProduct(productId));
    }

    @GetMapping("/product/{productId}/since")
    @Operation(summary = "Get price history since a date")
    public ResponseEntity<List<PriceHistoryResponse>> getHistorySince(
            @PathVariable Long productId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        return ResponseEntity.ok(priceHistoryService.getHistorySince(productId, since));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete price history record — Admin only")
    public ResponseEntity<Void> deletePriceHistory(@PathVariable Long id) {
        priceHistoryService.deletePriceHistory(id);
        return ResponseEntity.noContent().build();
    }
}
