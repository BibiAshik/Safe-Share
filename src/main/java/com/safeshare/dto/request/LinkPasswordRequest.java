package com.safeshare.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LinkPasswordRequest {

    @NotBlank(message = "Password is required")
    private String password;
}
