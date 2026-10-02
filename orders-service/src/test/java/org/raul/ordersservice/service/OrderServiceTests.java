package org.raul.ordersservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.raul.ordersservice.client.PaymentClient;
import org.raul.ordersservice.domain.OrderStatus;
import org.raul.ordersservice.domain.PaymentStatus;
import org.raul.ordersservice.dto.CreateOrderRequest;
import org.raul.ordersservice.dto.OrderDetailsResponse;
import org.raul.ordersservice.dto.OrderItemRequest;
import org.raul.ordersservice.dto.OrderItemResponse;
import org.raul.ordersservice.dto.PaymentRequest;
import org.raul.ordersservice.dto.PaymentResponse;
import org.raul.ordersservice.entity.Order;
import org.raul.ordersservice.entity.OrderItem;
import org.raul.ordersservice.entity.Product;
import org.raul.ordersservice.exception.InvalidOrderStateException;
import org.raul.ordersservice.exception.OrderNotFoundException;
import org.raul.ordersservice.exception.PaymentUnavailableException;
import org.raul.ordersservice.exception.ProductNotFoundException;
import org.raul.ordersservice.repository.OrderItemRepository;
import org.raul.ordersservice.repository.OrderRepository;
import org.raul.ordersservice.repository.ProductRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTests {

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderShouldSaveItemsAndCalculateTotal() {
        Product boots = new Product("Boots", new BigDecimal("9.99"), 10L);
        Product jacket = new Product("Jacket", new BigDecimal("49.99"), 11L);
        given(productRepository.findById(10L)).willReturn(Optional.of(boots));
        given(productRepository.findById(11L)).willReturn(Optional.of(jacket));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            if (order.getId() == null) {
                order.setId(20L);
            }
            return order;
        });
        given(orderItemRepository.save(any(OrderItem.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<OrderItem> savedItems = ArgumentCaptor.forClass(OrderItem.class);

        var response = orderService.createOrder(new CreateOrderRequest(
                123L,
                List.of(new OrderItemRequest(10L, 2), new OrderItemRequest(11L, 1))));
        verify(orderItemRepository, times(2)).save(savedItems.capture());

        assertEquals(20L, response.orderId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("69.97"), response.totalAmount());
        assertEquals(2, response.orderItems().size());
        assertEquals(2, response.orderItems().get(0).quantity());
        assertEquals(10L, response.orderItems().get(0).productId());
        assertEquals(new BigDecimal("9.99"), response.orderItems().get(0).unitPrice());
        boots.setPrice(new BigDecimal("12.99"));
        assertEquals(new BigDecimal("9.99"), savedItems.getAllValues().get(0).getUnitPrice());
        verify(orderRepository, times(2)).save(any(Order.class));
    }

    @Test
    void createOrderShouldThrowWhenProductDoesNotExist() {
        given(orderRepository.save(any(Order.class))).willAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(20L);
            return order;
        });
        given(productRepository.findById(404L)).willReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> orderService.createOrder(
                new CreateOrderRequest(123L, List.of(new OrderItemRequest(404L, 1)))));

        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    void getOrderDetailsShouldReturnExistingOrderAndItems() {
        Order order = order(20L, OrderStatus.PENDING);
        OrderItem item = new OrderItem(20L,
                new Product("Boots", new BigDecimal("9.99"), 45L), 2, new BigDecimal("9.99"));
        given(orderRepository.findById(20L)).willReturn(Optional.of(order));
        given(orderItemRepository.findByOrderId(20L)).willReturn(List.of(item));

        OrderDetailsResponse response = orderService.getOrdersDetails(20L);

        assertEquals(20L, response.orderId());
        assertEquals(123L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
        assertEquals(new BigDecimal("19.98"), response.totalAmount());
        assertEquals(List.of(new OrderItemResponse(null, 45L, 2, new BigDecimal("9.99"))),
                response.orderItems());
    }

    @Test
    void getOrderDetailsShouldThrowWhenOrderDoesNotExist() {
        given(orderRepository.findById(404L)).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.getOrdersDetails(404L));

        verify(orderItemRepository, never()).findByOrderId(404L);
    }

    @Test
    void payOrderShouldMarkOrderPaidWhenPaymentIsApproved() {
        Order order = order(20L, OrderStatus.PENDING);
        given(orderRepository.findById(20L)).willReturn(Optional.of(order));
        given(paymentClient.payOrder(any(PaymentRequest.class))).willReturn(PaymentResponse.paid(20L));

        PaymentResponse response = orderService.payOrder(20L);

        assertEquals(PaymentStatus.APPROVED, response.status());
        assertEquals(OrderStatus.PAID, order.getStatus());
        ArgumentCaptor<PaymentRequest> request = ArgumentCaptor.forClass(PaymentRequest.class);
        verify(paymentClient).payOrder(request.capture());
        assertEquals(20L, request.getValue().order_id());
        assertEquals(new BigDecimal("19.98"), request.getValue().amount());
        verify(orderRepository).save(order);
    }

    @Test
    void payOrderShouldLeaveOrderPendingWhenPaymentIsDeclined() {
        Order order = order(20L, OrderStatus.PENDING);
        given(orderRepository.findById(20L)).willReturn(Optional.of(order));
        given(paymentClient.payOrder(any(PaymentRequest.class)))
                .willReturn(PaymentResponse.declined(20L, "Card declined"));

        PaymentResponse response = orderService.payOrder(20L);

        assertEquals(PaymentStatus.DECLINED, response.status());
        assertEquals("Card declined", response.reason());
        assertEquals(OrderStatus.PENDING, order.getStatus());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void payOrderShouldThrowWhenOrderDoesNotExist() {
        given(orderRepository.findById(404L)).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.payOrder(404L));

        verify(paymentClient, never()).payOrder(any(PaymentRequest.class));
    }

    @Test
    void payOrderShouldThrowWhenOrderIsAlreadyPaid() {
        given(orderRepository.findById(20L)).willReturn(Optional.of(order(20L, OrderStatus.PAID)));

        assertThrows(InvalidOrderStateException.class, () -> orderService.payOrder(20L));

        verify(paymentClient, never()).payOrder(any(PaymentRequest.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void payOrderShouldPropagatePaymentServiceUnavailable() {
        given(orderRepository.findById(20L)).willReturn(Optional.of(order(20L, OrderStatus.PENDING)));
        given(paymentClient.payOrder(any(PaymentRequest.class)))
                .willThrow(new PaymentUnavailableException("Payment provider unreachable"));

        assertThrows(PaymentUnavailableException.class, () -> orderService.payOrder(20L));

        verify(orderRepository, never()).save(any(Order.class));
    }

    private static Order order(Long id, OrderStatus status) {
        Order order = new Order(123L, status, new BigDecimal("19.98"));
        order.setId(id);
        return order;
    }
}
