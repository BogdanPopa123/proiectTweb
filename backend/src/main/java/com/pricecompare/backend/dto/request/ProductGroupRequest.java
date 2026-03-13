package com.pricecompare.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Set;

@Data
public class ProductGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private Set<Long> productIds;
}
