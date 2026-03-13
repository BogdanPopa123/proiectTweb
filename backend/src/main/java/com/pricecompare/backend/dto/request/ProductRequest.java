package com.pricecompare.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    private String imageUrl;

    @NotBlank(message = "Product URL is required")
    private String productUrl;

    private String category;

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @Positive
    private BigDecimal currentPrice;

    private String currency = "RON";

    private Boolean active = true;
}
