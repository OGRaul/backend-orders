package org.raul.ordersservice.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

@SpringBootTest
public abstract class IntegrationTestBase {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        // Keep the database alive while Spring reuses its cached context across test classes.
        postgres.start();
    }

    static WireMockServer wireMock = new WireMockServer(0); // random free port

    @BeforeAll
    static void startWireMock() {
        wireMock.start();
    }

    @DynamicPropertySource
    static void paymentServiceUrl(DynamicPropertyRegistry registry) {
        registry.add("payment-service.base-url", () -> "http://localhost:" + wireMock.port());
    }

    @BeforeEach
    void resetWireMock() {
        configureFor("localhost", wireMock.port());
        wireMock.resetAll();
    }
}