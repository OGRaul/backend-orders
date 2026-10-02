package org.raul.ordersservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        Integer quantity,
        BigDecimal unitPrice
) {}
