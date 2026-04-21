package com.tynysai.gatewayserver.filter;

import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class JwtUserHeaderFilter implements GlobalFilter, Ordered {

    @Override
    public @NonNull Mono<Void> filter(@NonNull ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> Objects.requireNonNull(ctx.getAuthentication()))
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .map(jwt -> propagateHeaders(exchange, jwt))
                .defaultIfEmpty(exchange)
                .flatMap(chain::filter);
    }

    private ServerWebExchange propagateHeaders(ServerWebExchange exchange, Jwt jwt) {
        String userId = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String roles = extractRealmRoles(jwt);

        ServerHttpRequest.Builder mutated = exchange.getRequest().mutate()
                .headers(headers -> headers.remove("X-User-Id"))
                .headers(headers -> headers.remove("X-User-Email"))
                .headers(headers -> headers.remove("X-User-Roles"));

        if (userId != null) {
            mutated.header("X-User-Id", userId);
        }
        if (email != null) {
            mutated.header("X-User-Email", email);
        }
        if (!roles.isEmpty()) {
            mutated.header("X-User-Roles", roles);
        }

        return exchange.mutate().request(mutated.build()).build();
    }

    @SuppressWarnings("unchecked")
    private String extractRealmRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return "";
        }
        Object rolesClaim = realmAccess.get("roles");
        if (!(rolesClaim instanceof Collection<?>)) {
            return "";
        }
        return ((Collection<Object>) rolesClaim).stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}