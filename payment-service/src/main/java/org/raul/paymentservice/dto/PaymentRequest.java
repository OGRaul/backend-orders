package org.raul.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;


public record PaymentRequest(
        @NotNull Long orderId,
        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2) BigDecimal amount
) {}
