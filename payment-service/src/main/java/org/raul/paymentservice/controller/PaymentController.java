package org.raul.paymentservice.controller;

import jakarta.validation.Valid;
import org.raul.paymentservice.dto.PaymentRequest;
import org.raul.paymentservice.dto.PaymentResponse;
import org.raul.paymentservice.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService orderService) {
        this.paymentService = orderService;
    }

    /** Accepts an internal payment request and returns the simulated provider outcome. */
    @PostMapping
    public ResponseEntity<PaymentResponse> payOrder(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.payOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}