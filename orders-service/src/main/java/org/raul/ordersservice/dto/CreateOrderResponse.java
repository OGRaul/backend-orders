package org.raul.ordersservice.dto;



import org.raul.ordersservice.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderResponse(
        Long orderId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> orderItems
) {}
