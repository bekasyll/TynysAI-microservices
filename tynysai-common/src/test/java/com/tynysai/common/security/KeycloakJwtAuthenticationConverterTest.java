package com.tynysai.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {
    private final KeycloakJwtAuthenticationConverter converter = new KeycloakJwtAuthenticationConverter();

    @Test
    void convert_promotesKeycloakRealmRolesToRolePrefixedAuthorities() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("DOCTOR", "patient")))
                .build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        Set<String> auth = authoritiesOf(token);
        assertThat(auth).contains("ROLE_DOCTOR", "ROLE_PATIENT");
    }

    @Test
    void convert_includesScopeAuthorities_alongsideRealmRoles() {
        Jwt jwt = jwtBuilder()
                .claim("scope", "openid profile")
                .claim("realm_access", Map.of("roles", List.of("ADMIN")))
                .build();

        Set<String> auth = authoritiesOf(converter.convert(jwt));

        assertThat(auth).contains("ROLE_ADMIN", "SCOPE_openid", "SCOPE_profile");
    }

    @Test
    void convert_handlesMissingRealmAccess() {
        // No realm_access claim - only scope authorities should be present.
        Jwt jwt = jwtBuilder().claim("scope", "openid").build();

        Set<String> auth = authoritiesOf(converter.convert(jwt));

        assertThat(auth).containsExactly("SCOPE_openid");
    }

    @Test
    void convert_handlesEmptyRolesList() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of()))
                .build();

        Set<String> auth = authoritiesOf(converter.convert(jwt));

        assertThat(auth).noneMatch(a -> a.startsWith("ROLE_"));
    }

    @Test
    void convert_skipsNonStringRoles() {
        Jwt jwt = jwtBuilder()
                .claim("realm_access", Map.of("roles", List.of("DOCTOR", 42, "ADMIN")))
                .build();

        Set<String> auth = authoritiesOf(converter.convert(jwt));

        assertThat(auth).contains("ROLE_DOCTOR", "ROLE_ADMIN");
        assertThat(auth).doesNotContain("ROLE_42");
    }

    @Test
    void convert_setsSubjectAsPrincipalName() {
        String subject = "8b0e663e-7745-4a71-9024-243c2f44ed49";
        Jwt jwt = jwtBuilder().subject(subject).build();

        AbstractAuthenticationToken token = converter.convert(jwt);

        assertThat(token.getName()).isEqualTo(subject);
    }

    private static Jwt.Builder jwtBuilder() {
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-id")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60));
    }

    private static Set<String> authoritiesOf(AbstractAuthenticationToken token) {
        return token.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}