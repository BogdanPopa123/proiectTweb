package com.pricecompare.backend.dto.response;

import com.pricecompare.backend.entity.Store;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class StoreResponse {
    private Long id;
    private String name;
    private String baseUrl;
    private String logoUrl;
    private Boolean active;
    private LocalDateTime createdAt;

    public static StoreResponse from(Store store) {
        return StoreResponse.builder()
                .id(store.getId())
                .name(store.getName())
                .baseUrl(store.getBaseUrl())
                .logoUrl(store.getLogoUrl())
                .active(store.getActive())
                .createdAt(store.getCreatedAt())
                .build();
    }
}
