package com.pricecompare.backend.controller;

import com.pricecompare.backend.dto.request.AlertRequest;
import com.pricecompare.backend.dto.response.AlertResponse;
import com.pricecompare.backend.security.UserPrincipal;
import com.pricecompare.backend.service.AlertService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Alerts", description = "Price drop alert management")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    @Operation(summary = "Get current user's price alerts")
    public ResponseEntity<Page<AlertResponse>> getUserAlerts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(alertService.getUserAlerts(principal.getId(), pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alert by ID")
    public ResponseEntity<AlertResponse> getAlertById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(alertService.getAlertById(id, principal.getId()));
    }

    @PostMapping
    @Operation(summary = "Create a price drop alert")
    public ResponseEntity<AlertResponse> createAlert(
            @Valid @RequestBody AlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(alertService.createAlert(principal.getId(), request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update alert (target price or enabled status)")
    public ResponseEntity<AlertResponse> updateAlert(
            @PathVariable Long id,
            @Valid @RequestBody AlertRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(alertService.updateAlert(id, principal.getId(), request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete alert")
    public ResponseEntity<Void> deleteAlert(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        alertService.deleteAlert(id, principal.getId());
        return ResponseEntity.noContent().build();
    }
}
