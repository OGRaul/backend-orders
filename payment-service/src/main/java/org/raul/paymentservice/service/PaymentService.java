package org.raul.paymentservice.service;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.raul.paymentservice.dto.PaymentRequest;
import org.raul.paymentservice.dto.PaymentResponse;
import org.raul.paymentservice.domain.PaymentStatus;
import org.raul.paymentservice.entity.Payment;
import org.raul.paymentservice.exception.PaymentUnavailableException;
import org.raul.paymentservice.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    // Deterministic provider controls let tests exercise approval, decline and outage paths.
    private Boolean paymentServiceAvailable = true;
    private PaymentStatus paymentStatus = PaymentStatus.APPROVED;
    private String reason = "";

    public PaymentService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional
    public PaymentResponse payOrder(@Valid PaymentRequest request) {
        Payment payment = simulatePayment(request);

        // Persist business declines as payment outcomes. Provider unavailability throws before a payment is created.
        paymentRepository.save(payment);

        return toPaymentResponse(payment.getOrderId(), payment.getStatus(), payment.getReason());
    }

    private PaymentResponse toPaymentResponse(Long orderId, PaymentStatus status, String reason) {
        return new PaymentResponse(orderId, status, reason);
    }

    private Payment simulatePayment(PaymentRequest request) {
        Payment payment;
        if(paymentServiceAvailable) {
            payment = new Payment(request.orderId(), request.amount(), getPaymentStatus(), getReason());
        } else {
            throw new PaymentUnavailableException("Payment provider is currently unreachable");
        }
        return payment;
    }

    public Boolean getPaymentServiceAvailable() {
        return paymentServiceAvailable;
    }

    public void setPaymentServiceAvailable(Boolean paymentServiceAvailable) {
        this.paymentServiceAvailable = paymentServiceAvailable;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
