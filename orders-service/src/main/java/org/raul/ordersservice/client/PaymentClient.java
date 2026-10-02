package org.raul.ordersservice.client;

import org.raul.ordersservice.dto.PaymentRequest;
import org.raul.ordersservice.dto.PaymentResponse;
import org.raul.ordersservice.exception.PaymentUnavailableException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class PaymentClient {

    private final RestClient restClient;

    public PaymentClient(RestClient.Builder builder,
                         org.springframework.core.env.Environment env) {
        this.restClient = builder
                .baseUrl(env.getProperty("payment-service.base-url", "http://localhost:8081"))
                .build();
    }

    public PaymentResponse payOrder(PaymentRequest request) {
        try {
            return restClient.post()
                    .uri("/payments")
                    .body(request)
                    .retrieve()
                    .body(PaymentResponse.class);
        } catch (HttpServerErrorException ex) {
            throw new PaymentUnavailableException(
                    "Payment service returned HTTP " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new PaymentUnavailableException("Payment service is unreachable", ex);
        }
    }
}