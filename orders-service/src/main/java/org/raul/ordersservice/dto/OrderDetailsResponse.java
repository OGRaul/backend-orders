package org.raul.ordersservice.dto;


import org.raul.ordersservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderDetailsResponse(
        Long orderId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> orderItems
) {}
