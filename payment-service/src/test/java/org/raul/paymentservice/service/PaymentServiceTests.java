package org.raul.paymentservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.raul.paymentservice.domain.PaymentStatus;
import org.raul.paymentservice.dto.PaymentRequest;
import org.raul.paymentservice.exception.PaymentUnavailableException;
import org.raul.paymentservice.repository.PaymentRepository;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTests {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void payOrderShouldSaveApprovedPayment() {
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.payOrder(new PaymentRequest(45L, new BigDecimal("99.95")));

        assertEquals(45L, response.orderId());
        assertEquals(PaymentStatus.APPROVED, response.status());
        verify(paymentRepository).save(any());
    }

    @Test
    void payOrderShouldSaveDeclinedPaymentAndReason() {
        paymentService.setPaymentStatus(PaymentStatus.DECLINED);
        paymentService.setReason("Card declined");
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = paymentService.payOrder(new PaymentRequest(45L, new BigDecimal("99.95")));

        assertEquals(PaymentStatus.DECLINED, response.status());
        assertEquals("Card declined", response.reason());
        verify(paymentRepository).save(any());
    }

    @Test
    void payOrderShouldThrowPaymentUnavailableWhenProviderIsUnavailable() {
        paymentService.setPaymentServiceAvailable(false);

        assertThrows(PaymentUnavailableException.class,
                () -> paymentService.payOrder(new PaymentRequest(45L, new BigDecimal("99.95"))));

        verify(paymentRepository, never()).save(any());
    }
}
