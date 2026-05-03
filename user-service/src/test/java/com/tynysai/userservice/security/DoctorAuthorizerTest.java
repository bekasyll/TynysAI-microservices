package com.tynysai.userservice.security;

import com.tynysai.userservice.model.DoctorProfile;
import com.tynysai.userservice.repository.DoctorProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAuthorizerTest {
    @Mock
    private DoctorProfileRepository doctorProfileRepository;

    @InjectMocks
    private DoctorAuthorizer authorizer;

    private Jwt jwt(UUID subject) {
        return Jwt.withTokenValue("tok")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .subject(subject == null ? null : subject.toString())
                .claim("any", "v")
                .build();
    }

    @Test
    void canViewPatients_returnsTrue_forAdmin() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u", "p",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isTrue();
        verify(doctorProfileRepository, never()).findByUserId(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void canViewPatients_returnsFalse_whenNoDoctorRole() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u", "p",
                List.of(new SimpleGrantedAuthority("ROLE_PATIENT")));

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isFalse();
    }

    @Test
    void canViewPatients_returnsFalse_whenAuthenticationNull() {
        boolean ok = authorizer.canViewPatients(null);
        assertThat(ok).isFalse();
    }

    @Test
    void canViewPatients_returnsFalse_whenDoctorButNotJwt() {
        Authentication auth = new UsernamePasswordAuthenticationToken("u", "p",
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isFalse();
    }

    @Test
    void canViewPatients_returnsFalse_whenDoctorProfileMissing() {
        UUID id = UUID.randomUUID();
        Authentication auth = new JwtAuthenticationToken(jwt(id),
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        when(doctorProfileRepository.findByUserId(id)).thenReturn(Optional.empty());

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isFalse();
    }

    @Test
    void canViewPatients_returnsFalse_whenDoctorProfileNotApproved() {
        UUID id = UUID.randomUUID();
        Authentication auth = new JwtAuthenticationToken(jwt(id),
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        when(doctorProfileRepository.findByUserId(id))
                .thenReturn(Optional.of(DoctorProfile.builder().userId(id).approved(false).build()));

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isFalse();
    }

    @Test
    void canViewPatients_returnsTrue_whenDoctorProfileApproved() {
        UUID id = UUID.randomUUID();
        Authentication auth = new JwtAuthenticationToken(jwt(id),
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
        when(doctorProfileRepository.findByUserId(id))
                .thenReturn(Optional.of(DoctorProfile.builder().userId(id).approved(true).build()));

        boolean ok = authorizer.canViewPatients(auth);

        assertThat(ok).isTrue();
    }
}