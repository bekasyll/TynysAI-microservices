package com.tynysai.appointmentservice;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Needs Postgres + Kafka + config-server; gated until Testcontainers is wired in")
class AppointmentServiceApplicationTests {
    @Test
    void contextLoads() {
    }
}
