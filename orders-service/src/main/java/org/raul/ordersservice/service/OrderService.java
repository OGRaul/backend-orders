package org.raul.ordersservice.service;

import org.raul.ordersservice.client.PaymentClient;
import org.raul.ordersservice.domain.OrderStatus;
import org.raul.ordersservice.domain.PaymentStatus;
import org.raul.ordersservice.dto.*;
import org.raul.ordersservice.entity.Order;
import org.raul.ordersservice.entity.OrderItem;
import org.raul.ordersservice.entity.Product;
import org.raul.ordersservice.exception.InvalidOrderStateException;
import org.raul.ordersservice.exception.OrderNotFoundException;
import org.raul.ordersservice.exception.ProductNotFoundException;
import org.raul.ordersservice.repository.OrderItemRepository;
import org.raul.ordersservice.repository.OrderRepository;
import org.raul.ordersservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(PaymentClient paymentClient, OrderRepository orderRepository, ProductRepository productRepository, OrderItemRepository orderItemRepository) {
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        Order order = new Order(request.customerId(), OrderStatus.PENDING, BigDecimal.ZERO);
        List<OrderItem> orderItems = new ArrayList<>();

        BigDecimal total = BigDecimal.ZERO;

        Order saved = orderRepository.save(order);

        for (OrderItemRequest itemRequest : request.orderItems()) {
            Product product = productRepository.findById(itemRequest.productId())
                    .orElseThrow(() -> new ProductNotFoundException(itemRequest.productId()));

            // Keep the purchase price on the order item so future catalog changes do not reprice this order.
            OrderItem orderItem = new OrderItem(saved.getId(), product, itemRequest.quantity(), product.getPrice());


            OrderItem savedItem = orderItemRepository.save(orderItem);
            orderItems.add(savedItem);

            BigDecimal unitPrice = product.getPrice();

            total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemRequest.quantity())));
        }

        order.setTotalAmount(total);
        orderRepository.save(order);


        return toCreateOrderResponse(saved, toOrderItemResponses(orderItems));
    }

    private CreateOrderResponse toCreateOrderResponse(Order order, List<OrderItemResponse> orderItems) {
        return new CreateOrderResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                orderItems);
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponse getOrdersDetails(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);

        return toOrderDetailsResponse(order, toOrderItemResponses(orderItems));
    }

    private List<OrderItemResponse> toOrderItemResponses(List<OrderItem> orderItems) {
        return orderItems.stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getQuantity(),
                        item.getUnitPrice()))
                .toList();
    }

    private OrderDetailsResponse toOrderDetailsResponse(Order order, List<OrderItemResponse> orderItems) {
        return new OrderDetailsResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                orderItems
        );
    }

    @Transactional
    public PaymentResponse payOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Order " + orderId + " is not in PENDING state");
        }

        // Only approval advances the order; a decline leaves it PENDING for a possible retry.
        PaymentRequest paymentRequest = new PaymentRequest(orderId, order.getTotalAmount());
        PaymentResponse paymentResponse = paymentClient.payOrder(paymentRequest);

        if (paymentResponse.status() == PaymentStatus.APPROVED) {
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);
            return PaymentResponse.paid(orderId);
        }

        return PaymentResponse.declined(orderId, paymentResponse.reason());
    }
}