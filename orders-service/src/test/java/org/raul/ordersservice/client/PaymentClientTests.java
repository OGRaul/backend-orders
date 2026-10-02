package org.raul.ordersservice.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.raul.ordersservice.config.RestClientConfig;
import org.raul.ordersservice.dto.PaymentRequest;
import org.raul.ordersservice.exception.PaymentUnavailableException;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

class PaymentClientTests {

    private WireMockServer paymentService;
    private PaymentClient paymentClient;

    @BeforeEach
    void setUp() {
        paymentService = new WireMockServer(options().dynamicPort());
        paymentService.start();

        MockEnvironment environment = new MockEnvironment()
                .withProperty("payment-service.base-url", paymentService.baseUrl());
        paymentClient = new PaymentClient(new RestClientConfig().restClientBuilder(), environment);
    }

    @AfterEach
    void tearDown() {
        paymentService.stop();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 502, 503, 504})
    void payOrderShouldTranslateServerErrors(int statusCode) {
        paymentService.stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse().withStatus(statusCode)));

        PaymentUnavailableException exception = assertThrows(
                PaymentUnavailableException.class,
                () -> paymentClient.payOrder(new PaymentRequest(45L, new BigDecimal("99.95"))));

        assertTrue(exception.getMessage().contains(Integer.toString(statusCode)));
    }

    @Test
    void payOrderShouldTranslateConnectionFailures() {
        paymentService.stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse().withFault(
                        Fault.EMPTY_RESPONSE)));

        assertThrows(PaymentUnavailableException.class,
                () -> paymentClient.payOrder(new PaymentRequest(45L, new BigDecimal("99.95"))));
    }
}
