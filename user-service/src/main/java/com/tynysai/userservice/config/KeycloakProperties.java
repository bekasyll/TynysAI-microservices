package com.tynysai.userservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakProperties {
    private String serverUrl;
    private String realm;
    private AdminClient adminClient = new AdminClient();

    @Getter
    @Setter
    public static class AdminClient {
        private String clientId;
        private String clientSecret;
    }
}