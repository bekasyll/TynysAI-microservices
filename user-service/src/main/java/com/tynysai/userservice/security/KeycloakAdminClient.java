package com.tynysai.userservice.security;

import com.tynysai.userservice.config.KeycloakAdminProperties;
import com.tynysai.userservice.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class KeycloakAdminClient {
    private final KeycloakAdminProperties keycloakAdminProperties;
    private final RestClient http;
    private final AtomicReference<TokenSnapshot> token = new AtomicReference<>();

    public KeycloakAdminClient(KeycloakAdminProperties keycloakAdminProperties) {
        this.keycloakAdminProperties = keycloakAdminProperties;
        this.http = RestClient.builder()
                .baseUrl(keycloakAdminProperties.baseUrl())
                .build();
    }

    public UUID createUser(String email,
                           String firstName,
                           String lastName,
                           String password,
                           boolean emailVerified,
                           boolean passwordTemporary) {
        Map<String, Object> body = Map.of(
                "username", email,
                "email", email,
                "firstName", firstName,
                "lastName", lastName,
                "enabled", true,
                "emailVerified", emailVerified,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", passwordTemporary))
        );

        try {
            ResponseEntity<Void> resp = http.post()
                    .uri("/admin/realms/{realm}/users", keycloakAdminProperties.realm())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            URI location = resp.getHeaders().getLocation();
            if (location == null) {
                throw new IllegalStateException("Keycloak did not return a Location header for the new user");
            }
            String path = location.getPath();
            String idStr = path.substring(path.lastIndexOf('/') + 1);
            return UUID.fromString(idStr);
        } catch (HttpClientErrorException.Conflict e) {
            throw new BadRequestException("Email is already registered");
        } catch (HttpClientErrorException e) {
            log.warn("Keycloak rejected createUser for {}: {} {}", email, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadRequestException("Could not create account: " + e.getStatusText());
        }
    }

    /**
     * Assigns a realm role (e.g. {@code DOCTOR}) to the given user. The
     * default role {@code PATIENT} is already assigned automatically by the
     * realm config, so this is only needed for non-default roles.
     */
    public void assignRealmRole(UUID userId, String roleName) {
        Map<String, Object> roleRep = http.get()
                .uri("/admin/realms/{realm}/roles/{name}", keycloakAdminProperties.realm(), roleName)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .body(Map.class);

        http.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(roleRep))
                .retrieve()
                .toBodilessEntity();
    }

    public void deleteUserQuietly(UUID userId) {
        try {
            deleteUser(userId);
        } catch (Exception e) {
            log.warn("Failed to roll back Keycloak user {}: {}", userId, e.getMessage());
        }
    }

    public void deleteUser(UUID userId) {
        http.delete()
                .uri("/admin/realms/{realm}/users/{id}", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .toBodilessEntity();
    }

    public void setUserEnabled(UUID userId, boolean enabled) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("enabled", enabled))
                .retrieve()
                .toBodilessEntity();
    }

    public void resetPassword(UUID userId, String newPassword, boolean temporary) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "type", "password",
                        "value", newPassword,
                        "temporary", temporary))
                .retrieve()
                .toBodilessEntity();
    }

    public void sendVerifyEmail(UUID userId) {
        http.put()
                .uri("/admin/realms/{realm}/users/{id}/send-verify-email", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .toBodilessEntity();
    }

    public boolean verifyPassword(String email, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", keycloakAdminProperties.frontendClientId());
        form.add("username", email);
        form.add("password", password);
        form.add("scope", "openid");

        try {
            http.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", keycloakAdminProperties.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (HttpClientErrorException e) {
            return false;
        }
    }

    public void logoutAllSessions(UUID userId) {
        http.post()
                .uri("/admin/realms/{realm}/users/{id}/logout", keycloakAdminProperties.realm(), userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken())
                .retrieve()
                .toBodilessEntity();
    }

    private String adminToken() {
        TokenSnapshot current = token.get();
        if (current != null && current.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
            return current.value;
        }
        return token.updateAndGet(prev -> {
            if (prev != null && prev.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
                return prev;
            }
            return fetchToken();
        }).value;
    }

    private TokenSnapshot fetchToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", keycloakAdminProperties.clientId());
        form.add("client_secret", keycloakAdminProperties.clientSecret());

        Map<String, Object> body = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", keycloakAdminProperties.realm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (body == null || !(body.get("access_token") instanceof String accessToken)) {
            throw new IllegalStateException("Keycloak did not return an access_token for the admin client");
        }
        Number expiresIn = (Number) body.getOrDefault("expires_in", 60);
        Instant expiresAt = Instant.now().plus(Duration.ofSeconds(expiresIn.longValue()));
        log.debug("Fetched fresh admin token, expires in {}s", expiresIn);
        return new TokenSnapshot(accessToken, expiresAt);
    }

    private record TokenSnapshot(String value, Instant expiresAt) {
    }
}