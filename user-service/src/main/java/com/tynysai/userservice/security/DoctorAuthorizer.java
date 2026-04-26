package com.tynysai.userservice.security;

import com.tynysai.userservice.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("doctorAuthorizer")
@RequiredArgsConstructor
public class DoctorAuthorizer {
    private final DoctorProfileRepository doctorProfileRepository;

    @Transactional(readOnly = true)
    public boolean canViewPatients(Authentication auth) {
        if (hasRole(auth, "ROLE_ADMIN")) return true;
        if (!hasRole(auth, "ROLE_DOCTOR")) return false;

        UUID userId = currentUserId(auth);
        if (userId == null) return false;

        return doctorProfileRepository.findByUserId(userId)
                .map(d -> d.isApproved())
                .orElse(false);
    }

    private static boolean hasRole(Authentication auth, String role) {
        if (auth == null) return false;
        for (GrantedAuthority a : auth.getAuthorities()) {
            if (role.equals(a.getAuthority())) return true;
        }
        return false;
    }

    private static UUID currentUserId(Authentication auth) {
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            Jwt jwt = jwtAuth.getToken();
            if (jwt != null && jwt.getSubject() != null) {
                return UUID.fromString(jwt.getSubject());
            }
        }
        return null;
    }
}
