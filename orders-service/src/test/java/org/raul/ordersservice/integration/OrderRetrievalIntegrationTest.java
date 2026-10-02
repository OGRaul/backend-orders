package org.raul.ordersservice.integration;

import org.junit.jupiter.api.Test;
import org.raul.ordersservice.domain.OrderStatus;
import org.raul.ordersservice.entity.Order;
import org.raul.ordersservice.entity.OrderItem;
import org.raul.ordersservice.entity.Product;
import org.raul.ordersservice.repository.OrderItemRepository;
import org.raul.ordersservice.repository.OrderRepository;
import org.raul.ordersservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Sql("/sql/order-test-data.sql")
class OrderRetrievalIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderItemRepository orderItemRepository;

    @Autowired
    ProductRepository productRepository;

    @Test
    void existingOrderReturns200() throws Exception {
        Order saved = orderRepository.save(new Order(123L, OrderStatus.PENDING, new BigDecimal("39.98")));
        Product product = productRepository.findById(45L).orElseThrow();
        OrderItem savedItem = orderItemRepository.save(
                new OrderItem(saved.getId(), product, 2, new BigDecimal("19.99")));

        mockMvc.perform(get("/orders/{id}", saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.order_id").value(saved.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.order_items[0].order_item_id").value(savedItem.getId()))
                .andExpect(jsonPath("$.order_items[0].quantity").value(2))
                .andExpect(jsonPath("$.order_items[0].unit_price").value(19.99))
                .andExpect(jsonPath("$.order_items[0].product_id").value(45));
    }

    @Test
    void unknownOrderReturns404() throws Exception {
        mockMvc.perform(get("/orders/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}