package com.pricecompare.backend.dto.response;

import com.pricecompare.backend.entity.ProductGroup;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class ProductGroupResponse {
    private Long id;
    private String name;
    private List<ProductResponse> products;
    private LocalDateTime createdAt;

    public static ProductGroupResponse from(ProductGroup group) {
        return ProductGroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .products(group.getProducts() != null
                        ? group.getProducts().stream()
                            .map(ProductResponse::from)
                            .collect(Collectors.toList())
                        : List.of())
                .createdAt(group.getCreatedAt())
                .build();
    }
}
