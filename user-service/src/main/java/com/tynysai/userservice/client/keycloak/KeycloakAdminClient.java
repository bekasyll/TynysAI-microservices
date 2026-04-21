package com.tynysai.userservice.client.keycloak;

import com.tynysai.userservice.client.keycloak.dto.KeycloakRoleRepresentation;
import com.tynysai.userservice.client.keycloak.dto.KeycloakUserRepresentation;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(
        name = "keycloak-admin",
        url = "${keycloak.server-url}"
)
public interface KeycloakAdminClient {

    @PostMapping("/admin/realms/{realm}/users")
    ResponseEntity<Void> createUser(@RequestHeader("Authorization") String bearer,
                                    @PathVariable("realm") String realm,
                                    @RequestBody KeycloakUserRepresentation user);

    @DeleteMapping("/admin/realms/{realm}/users/{id}")
    void deleteUser(@RequestHeader("Authorization") String bearer,
                    @PathVariable("realm") String realm,
                    @PathVariable("id") String id);

    @GetMapping("/admin/realms/{realm}/roles/{role-name}")
    KeycloakRoleRepresentation getRealmRole(@RequestHeader("Authorization") String bearer,
                                            @PathVariable("realm") String realm,
                                            @PathVariable("role-name") String roleName);

    @PostMapping("/admin/realms/{realm}/users/{id}/role-mappings/realm")
    void assignRealmRoles(@RequestHeader("Authorization") String bearer,
                          @PathVariable("realm") String realm,
                          @PathVariable("id") String id,
                          @RequestBody List<KeycloakRoleRepresentation> roles);
}