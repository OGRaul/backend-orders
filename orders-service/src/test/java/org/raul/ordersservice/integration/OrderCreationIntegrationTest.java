package org.raul.ordersservice.integration;

import org.junit.jupiter.api.Test;
import org.raul.ordersservice.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Sql("/sql/order-test-data.sql")
class OrderCreationIntegrationTest extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    OrderRepository orderRepository;

    @Test
    void validOrderReturns201() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"customer_id": 123, "order_items": [{"product_id": 45, "quantity": 2}, {"product_id": 2, "quantity": 1}]}
                            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total_amount").value(69.97))
                .andExpect(jsonPath("$.order_items[0].quantity").value(2));
    }

    @Test
    void invalidRequestReturns400() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"customer_id": null, "order_items": []}
                            """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownProductReturns404() throws Exception {
        long orderCountBefore = orderRepository.count();

        mockMvc.perform(post("/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"customer_id": 123, "order_items": [{"product_id": 999999, "quantity": 1}]}
                            """))
                .andExpect(status().isNotFound());

        assertEquals(orderCountBefore, orderRepository.count());
    }
}