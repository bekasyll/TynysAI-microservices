package com.tynysai.xrayservice.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final JwtGrantedAuthoritiesConverter scopesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> scopeAuthorities = scopesConverter.convert(jwt);
        Set<GrantedAuthority> all = new HashSet<>(scopeAuthorities == null ? List.of() : scopeAuthorities);

        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof Collection<?> rolesList) {
                rolesList.stream()
                        .filter(r -> r instanceof String)
                        .map(r -> "ROLE_" + ((String) r).toUpperCase())
                        .map(SimpleGrantedAuthority::new)
                        .forEach(all::add);
            }
        }
        return new JwtAuthenticationToken(jwt, all, jwt.getSubject());
    }
}