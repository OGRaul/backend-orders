package org.raul.ordersservice.integration;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.Test;
import org.raul.ordersservice.domain.OrderStatus;
import org.raul.ordersservice.entity.Order;
import org.raul.ordersservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PaymentIntegrationTest extends IntegrationTestBase {

    @Autowired MockMvc mockMvc;
    @Autowired
    OrderRepository orderRepository;

    @Test
    void approvedPaymentMarksOrderPaid() throws Exception {
        Order order = orderRepository.save(new Order(123L, OrderStatus.PENDING, new BigDecimal("19.98")));

        stubFor(WireMock.post(urlEqualTo("/payments"))
                .willReturn(okJson("""
                    {"payment_id": 1, "order_id": %d, "status": "APPROVED"}
                    """.formatted(order.getId()))));

        mockMvc.perform(post("/orders/{id}/pay", order.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(OrderStatus.PAID,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void declinedPaymentKeepsOrderPending() throws Exception {
        Order order = orderRepository.save(new Order(123L, OrderStatus.PENDING, new BigDecimal("19.98")));

        stubFor(WireMock.post(urlEqualTo("/payments"))
                .willReturn(okJson("""
                    {"payment_id": 2, "order_id": %d, "status": "DECLINED", "reason": "insufficient funds"}
                    """.formatted(order.getId()))));

        mockMvc.perform(post("/orders/{id}/pay", order.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("insufficient funds"));

        assertEquals(OrderStatus.PENDING,
                orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void nonPendingOrderReturns409() throws Exception {
        Order order = orderRepository.save(new Order(123L, OrderStatus.PAID, new BigDecimal("19.98")));

        mockMvc.perform(post("/orders/{id}/pay", order.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentServiceUnavailableReturnsUpstreamError() throws Exception {
        Order order = orderRepository.save(new Order(123L, OrderStatus.PENDING, new BigDecimal("19.98")));

        stubFor(WireMock.post(urlEqualTo("/payments")).willReturn(aResponse().withStatus(503)));

        mockMvc.perform(post("/orders/{id}/pay", order.getId()))
                .andExpect(status().isServiceUnavailable()); // matches your PaymentUnavailableException handler
    }
}