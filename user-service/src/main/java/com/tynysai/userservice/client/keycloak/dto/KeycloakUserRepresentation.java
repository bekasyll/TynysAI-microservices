package com.tynysai.userservice.client.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class KeycloakUserRepresentation {
    private String email;
    private String firstName;
    private String lastName;
    private Boolean enabled;
    private Boolean emailVerified;
    private List<Credential> credentials;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Credential {
        private String type;
        private String value;
        private Boolean temporary;
    }
}