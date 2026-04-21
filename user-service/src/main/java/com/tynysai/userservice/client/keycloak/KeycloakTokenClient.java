package com.tynysai.userservice.client.keycloak;

import com.tynysai.userservice.client.keycloak.dto.KeycloakTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "keycloak-token",
        url = "${keycloak.server-url}",
        configuration = KeycloakTokenClient.Config.class
)
public interface KeycloakTokenClient {

    @PostMapping(
            value = "/realms/{realm}/protocol/openid-connect/token",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE
    )
    KeycloakTokenResponse fetchToken(@PathVariable("realm") String realm,
                                     @RequestBody String formBody);

    class Config {
        // Feign form encoding is fine with plain String body when consumes is form-urlencoded
    }
}