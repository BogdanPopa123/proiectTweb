package com.pricecompare.backend.dto.response;

import com.pricecompare.backend.entity.Alert;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AlertResponse {
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private BigDecimal targetPrice;
    private BigDecimal currentPrice;
    private String storeName;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime triggeredAt;

    public static AlertResponse from(Alert alert) {
        return AlertResponse.builder()
                .id(alert.getId())
                .userId(alert.getUser() != null ? alert.getUser().getId() : null)
                .productId(alert.getProduct() != null ? alert.getProduct().getId() : null)
                .productName(alert.getProduct() != null ? alert.getProduct().getName() : null)
                .productImageUrl(alert.getProduct() != null ? alert.getProduct().getImageUrl() : null)
                .targetPrice(alert.getTargetPrice())
                .currentPrice(alert.getProduct() != null ? alert.getProduct().getCurrentPrice() : null)
                .storeName(alert.getProduct() != null && alert.getProduct().getStore() != null
                        ? alert.getProduct().getStore().getName() : null)
                .enabled(alert.getEnabled())
                .createdAt(alert.getCreatedAt())
                .triggeredAt(alert.getTriggeredAt())
                .build();
    }
}
