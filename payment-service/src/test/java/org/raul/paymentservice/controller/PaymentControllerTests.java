package org.raul.paymentservice.controller;

import org.junit.jupiter.api.Test;
import org.raul.paymentservice.domain.PaymentStatus;
import org.raul.paymentservice.dto.PaymentRequest;
import org.raul.paymentservice.dto.PaymentResponse;
import org.raul.paymentservice.exception.PaymentUnavailableException;
import org.raul.paymentservice.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import(PaymentExceptionHandler.class)
class PaymentControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void payShouldReturnCreatedResponseWhenPaymentIsApproved() throws Exception {
        given(paymentService.payOrder(any(PaymentRequest.class)))
                .willReturn(new PaymentResponse(45L, PaymentStatus.APPROVED, ""));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new PaymentRequest(45L, new BigDecimal("99.95")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value(45))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void payShouldReturnCreatedResponseWhenPaymentIsDeclined() throws Exception {
        given(paymentService.payOrder(any(PaymentRequest.class)))
                .willReturn(new PaymentResponse(45L, PaymentStatus.DECLINED, "Card declined"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"order_id":45,"amount":99.95}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value(45))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Card declined"));
    }

    @Test
    void payShouldRejectInvalidRequest() throws Exception {
        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"order_id":45,"amount":0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void payShouldReturnServiceUnavailableWhenProviderIsUnavailable() throws Exception {
        given(paymentService.payOrder(any(PaymentRequest.class)))
                .willThrow(new PaymentUnavailableException("Payment provider is currently unreachable"));

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"order_id":45,"amount":99.95}
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("Payment service unavailable"))
                .andExpect(jsonPath("$.detail").value("Payment provider is currently unreachable"));
    }
}
