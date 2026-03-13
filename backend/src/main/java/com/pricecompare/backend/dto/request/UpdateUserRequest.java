package com.pricecompare.backend.dto.request;

import com.pricecompare.backend.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(max = 100)
    private String fullName;

    @Email
    private String email;

    @Size(min = 6)
    private String password;

    private UserRole role;
}
