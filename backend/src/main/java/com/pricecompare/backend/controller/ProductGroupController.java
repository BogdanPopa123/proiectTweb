package com.pricecompare.backend.controller;

import com.pricecompare.backend.dto.request.ProductGroupRequest;
import com.pricecompare.backend.dto.response.ProductGroupResponse;
import com.pricecompare.backend.service.ProductGroupService;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product-groups")
@RequiredArgsConstructor
@Tag(name = "Product Groups", description = "Cross-site product grouping for price comparison")
public class ProductGroupController {

    private final ProductGroupService productGroupService;

    @GetMapping
    @Operation(summary = "List product groups (paginated)")
    public ResponseEntity<Page<ProductGroupResponse>> getAllGroups(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(productGroupService.getAllGroups(search, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product group with all products")
    public ResponseEntity<ProductGroupResponse> getGroupById(@PathVariable Long id) {
        return ResponseEntity.ok(productGroupService.getGroupById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create product group — Admin only")
    public ResponseEntity<ProductGroupResponse> createGroup(
            @Valid @RequestBody ProductGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productGroupService.createGroup(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update product group — Admin only")
    public ResponseEntity<ProductGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody ProductGroupRequest request) {
        return ResponseEntity.ok(productGroupService.updateGroup(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete product group — Admin only")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        productGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }
}
