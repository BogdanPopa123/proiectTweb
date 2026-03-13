package com.pricecompare.backend.dto.response;

import com.pricecompare.backend.entity.PriceHistory;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PriceHistoryResponse {
    private Long id;
    private Long productId;
    private BigDecimal price;
    private String currency;
    private LocalDateTime recordedAt;

    public static PriceHistoryResponse from(PriceHistory ph) {
        return PriceHistoryResponse.builder()
                .id(ph.getId())
                .productId(ph.getProduct() != null ? ph.getProduct().getId() : null)
                .price(ph.getPrice())
                .currency(ph.getCurrency())
                .recordedAt(ph.getRecordedAt())
                .build();
    }
}
