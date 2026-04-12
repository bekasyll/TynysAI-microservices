package com.tynysai.userservice.dto.response;

import com.tynysai.userservice.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String phoneNumber;
    private Role role;
    private boolean enabled;
    private boolean emailVerified;
    private String avatarPath;
    private LocalDateTime createdAt;
}
