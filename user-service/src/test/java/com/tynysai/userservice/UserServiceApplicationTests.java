package com.tynysai.userservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Needs Postgres + Keycloak + config-server; gated until Testcontainers is wired in")
class UserServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
