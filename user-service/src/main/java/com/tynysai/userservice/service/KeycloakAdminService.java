package com.tynysai.userservice.service;

import com.tynysai.userservice.client.keycloak.KeycloakAdminClient;
import com.tynysai.userservice.client.keycloak.KeycloakTokenClient;
import com.tynysai.userservice.client.keycloak.dto.KeycloakRoleRepresentation;
import com.tynysai.userservice.client.keycloak.dto.KeycloakTokenResponse;
import com.tynysai.userservice.client.keycloak.dto.KeycloakUserRepresentation;
import com.tynysai.userservice.config.KeycloakProperties;
import com.tynysai.userservice.exception.BadRequestException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {
    private static final long TOKEN_SAFETY_MARGIN_SECONDS = 30;

    private final KeycloakTokenClient tokenClient;
    private final KeycloakAdminClient adminClient;
    private final KeycloakProperties properties;

    private volatile String cachedToken;
    private volatile Instant tokenExpiresAt = Instant.EPOCH;

    /**
     * Creates a user in Keycloak and returns the new user's UUID (from the Location header).
     * The user gets realm default roles automatically (PATIENT via default-roles-tynysai composite).
     */
    public UUID createUser(String email, String password, String firstName, String lastName) {
        return createUserInternal(email, password, firstName, lastName, false);
    }

    /**
     * Admin-driven creation: creates user in Keycloak with a temporary password
     * (Keycloak will force the user to change it on first login) and assigns the specified realm role.
     */
    public UUID createUserWithRole(String email, String password, String firstName, String lastName,
                                    String realmRoleName) {
        UUID keycloakId = createUserInternal(email, password, firstName, lastName, true);
        try {
            KeycloakRoleRepresentation role = adminClient.getRealmRole(
                    "Bearer " + getAdminToken(),
                    properties.getRealm(),
                    realmRoleName);
            adminClient.assignRealmRoles(
                    "Bearer " + getAdminToken(),
                    properties.getRealm(),
                    keycloakId.toString(),
                    List.of(role));
            return keycloakId;
        } catch (FeignException e) {
            log.error("Failed to assign role '{}' to Keycloak user {}: {} / {}",
                    realmRoleName, keycloakId, e.status(), e.contentUTF8());
            deleteUserQuietly(keycloakId);
            throw new IllegalStateException("Keycloak role assignment failed", e);
        }
    }

    private UUID createUserInternal(String email, String password, String firstName, String lastName,
                                    boolean temporaryPassword) {
        KeycloakUserRepresentation user = KeycloakUserRepresentation.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .enabled(true)
                .emailVerified(true)
                .credentials(List.of(KeycloakUserRepresentation.Credential.builder()
                        .type("password")
                        .value(password)
                        .temporary(temporaryPassword)
                        .build()))
                .build();

        try {
            ResponseEntity<Void> response = adminClient.createUser(
                    "Bearer " + getAdminToken(),
                    properties.getRealm(),
                    user);

            String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
            if (location == null) {
                throw new IllegalStateException("Keycloak did not return Location header on user creation");
            }
            return extractIdFromLocation(location);
        } catch (FeignException.Conflict e) {
            throw new BadRequestException("User with this email already exists");
        } catch (FeignException e) {
            log.error("Failed to create user in Keycloak: {} / {}", e.status(), e.contentUTF8());
            throw new IllegalStateException("Keycloak user creation failed", e);
        }
    }

    /**
     * Best-effort rollback if backend save fails after Keycloak user was created.
     */
    public void deleteUserQuietly(UUID keycloakId) {
        try {
            adminClient.deleteUser("Bearer " + getAdminToken(),
                    properties.getRealm(),
                    keycloakId.toString());
        } catch (Exception e) {
            log.warn("Failed to rollback Keycloak user {}: {}", keycloakId, e.getMessage());
        }
    }

    private synchronized String getAdminToken() {
        if (cachedToken != null && Instant.now().isBefore(tokenExpiresAt)) {
            return cachedToken;
        }
        KeycloakProperties.AdminClient client = properties.getAdminClient();
        String form = "grant_type=client_credentials"
                + "&client_id=" + URLEncoder.encode(client.getClientId(), StandardCharsets.UTF_8)
                + "&client_secret=" + URLEncoder.encode(client.getClientSecret(), StandardCharsets.UTF_8);

        KeycloakTokenResponse response = tokenClient.fetchToken(properties.getRealm(), form);
        cachedToken = response.getAccessToken();
        tokenExpiresAt = Instant.now().plusSeconds(response.getExpiresIn() - TOKEN_SAFETY_MARGIN_SECONDS);
        return cachedToken;
    }

    private static UUID extractIdFromLocation(String location) {
        URI uri = URI.create(location);
        String path = uri.getPath();
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            throw new IllegalStateException("Unexpected Location header: " + location);
        }
        return UUID.fromString(path.substring(lastSlash + 1));
    }
}