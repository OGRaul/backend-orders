package org.raul.ordersservice.controller;

import jakarta.validation.Valid;
import org.raul.ordersservice.domain.PaymentStatus;
import org.raul.ordersservice.dto.CreateOrderRequest;
import org.raul.ordersservice.dto.CreateOrderResponse;
import org.raul.ordersservice.dto.OrderDetailsResponse;
import org.raul.ordersservice.dto.PaymentResponse;
import org.raul.ordersservice.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Creates a PENDING order from a validated request. */
    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        CreateOrderResponse response = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Returns the order details, including the prices captured when it was created. */
    @GetMapping("/{order_id}")
    public ResponseEntity<OrderDetailsResponse> getOrdersDetails(@PathVariable("order_id") Long orderId) {
        OrderDetailsResponse response = orderService.getOrdersDetails(orderId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    /** Returns 200 for an approved payment and 409 for a declined payment. */
    @PostMapping("/{order_id}/pay")
    public ResponseEntity<PaymentResponse> payOrder(@PathVariable("order_id") Long orderId) {
        PaymentResponse response = orderService.payOrder(orderId);

        HttpStatus status = response.status() == PaymentStatus.APPROVED
                ? HttpStatus.OK
                : HttpStatus.CONFLICT;

        return ResponseEntity.status(status).body(response);
    }
}