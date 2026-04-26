package com.tynysai.userservice.dto.response;

import com.tynysai.userservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private UUID userId;
    private String email;
    private Role role;
    private boolean approved;
}
