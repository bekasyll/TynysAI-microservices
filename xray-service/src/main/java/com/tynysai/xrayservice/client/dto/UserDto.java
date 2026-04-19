package com.tynysai.xrayservice.client.dto;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phoneNumber;
    private String role;
    private boolean enabled;
}
