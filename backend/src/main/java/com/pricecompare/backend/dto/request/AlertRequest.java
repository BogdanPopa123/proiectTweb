package com.pricecompare.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AlertRequest {

    @NotNull(message = "Product ID is required")
    private Long productId;

    private BigDecimal targetPrice;

    private Boolean enabled = true;
}
