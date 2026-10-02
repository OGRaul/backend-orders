package org.raul.ordersservice.controller;

import org.junit.jupiter.api.Test;
import org.raul.ordersservice.domain.OrderStatus;
import org.raul.ordersservice.dto.CreateOrderRequest;
import org.raul.ordersservice.dto.CreateOrderResponse;
import org.raul.ordersservice.dto.OrderDetailsResponse;
import org.raul.ordersservice.dto.OrderItemRequest;
import org.raul.ordersservice.dto.OrderItemResponse;
import org.raul.ordersservice.dto.PaymentResponse;
import org.raul.ordersservice.exception.InvalidOrderStateException;
import org.raul.ordersservice.exception.OrderNotFoundException;
import org.raul.ordersservice.exception.PaymentUnavailableException;
import org.raul.ordersservice.exception.ProductNotFoundException;
import org.raul.ordersservice.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrderShouldReturnCreatedResponse() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                123L,
                List.of(
                        new OrderItemRequest(45L, 2),
                        new OrderItemRequest(2L, 1)
        ));

        CreateOrderResponse response = new CreateOrderResponse(
                2L,
                OrderStatus.PENDING,
                new BigDecimal("69.97"),
                List.of(
                        new OrderItemResponse(4L, 45L, 2, new BigDecimal("9.99")),
                        new OrderItemResponse(5L, 2L, 1, new BigDecimal("49.99"))
                )
        );

        given(orderService.createOrder(any(CreateOrderRequest.class))).willReturn(response);

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.order_id").value(2L))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total_amount").value(new BigDecimal("69.97")))
                .andExpect(jsonPath("$.order_items[0].order_item_id").value(4))
                .andExpect(jsonPath("$.order_items[0].product_id").value(45))
                .andExpect(jsonPath("$.order_items[0].unit_price").value(9.99))
                .andExpect(jsonPath("$.order_items[1].quantity").value(1));

        verify(orderService).createOrder(any(CreateOrderRequest.class));
    }

    @Test
    void createOrderShouldRejectInvalidInput() throws Exception {
        String invalidJson = """
                {
                  "customer_id": 123,
                  "order_items": []
                }
                """;

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(orderService);
    }

    @Test
    void createOrderShouldReturnNotFoundWhenProductDoesNotExist() throws Exception {
        given(orderService.createOrder(any(CreateOrderRequest.class)))
                .willThrow(new ProductNotFoundException(34234L));

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customer_id":123,"order_items":[{"product_id":34234,"quantity":1}]}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product not found: 34234"));
    }

    @Test
    void getOrderShouldReturnExistingOrder() throws Exception {
        OrderDetailsResponse response = new OrderDetailsResponse(
                3L, 123L, OrderStatus.PENDING, new BigDecimal("69.97"), List.of());
        given(orderService.getOrdersDetails(3L)).willReturn(response);

        mockMvc.perform(get("/orders/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(3))
                .andExpect(jsonPath("$.customer_id").value(123))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total_amount").value(69.97));
    }

    @Test
    void getOrderShouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        given(orderService.getOrdersDetails(404L)).willThrow(new OrderNotFoundException(404L));

        mockMvc.perform(get("/orders/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found: 404"));
    }

    @Test
    void payOrderShouldReturnOkWhenPaymentIsApproved() throws Exception {
        given(orderService.payOrder(3L)).willReturn(PaymentResponse.paid(3L));

        mockMvc.perform(post("/orders/3/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(3))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void payOrderShouldReturnConflictWhenPaymentIsDeclined() throws Exception {
        given(orderService.payOrder(3L)).willReturn(PaymentResponse.declined(3L, "Card declined"));

        mockMvc.perform(post("/orders/3/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.order_id").value(3))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.reason").value("Card declined"));
    }

    @Test
    void payOrderShouldReturnNotFoundWhenOrderDoesNotExist() throws Exception {
        given(orderService.payOrder(404L)).willThrow(new OrderNotFoundException(404L));

        mockMvc.perform(post("/orders/404/pay"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Order not found: 404"));
    }

    @Test
    void payOrderShouldReturnConflictWhenOrderIsNotPending() throws Exception {
        given(orderService.payOrder(3L))
                .willThrow(new InvalidOrderStateException("Order 3 is not in PENDING state"));

        mockMvc.perform(post("/orders/3/pay"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Order 3 is not in PENDING state"));
    }

    @Test
    void payOrderShouldReturnServiceUnavailableWhenPaymentServiceIsUnavailable() throws Exception {
        given(orderService.payOrder(3L))
                .willThrow(new PaymentUnavailableException("Payment provider unreachable"));

        mockMvc.perform(post("/orders/3/pay"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Payment provider unreachable"));
    }
}
