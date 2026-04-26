package com.tynysai.userservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminResetPasswordRequest {
    @NotBlank
    @Size(min = 8, max = 64)
    private String newPassword;

    private boolean temporary = true;
}