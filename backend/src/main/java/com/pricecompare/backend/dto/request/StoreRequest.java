package com.pricecompare.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StoreRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Base URL is required")
    private String baseUrl;

    private String logoUrl;

    private Boolean active = true;
}
