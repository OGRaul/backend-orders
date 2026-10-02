package org.raul.ordersservice.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import org.raul.ordersservice.domain.PaymentStatus;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaymentResponse(
        Long orderId,
        PaymentStatus status,
        String reason
) {
    public static PaymentResponse paid(Long orderId) {
        return new PaymentResponse(orderId, PaymentStatus.APPROVED, null);
    }

    public static PaymentResponse declined(Long orderId, String reason) {
        return new PaymentResponse(orderId, PaymentStatus.DECLINED, reason);
    }
}
