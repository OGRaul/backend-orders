package org.raul.ordersservice.repository;

import org.raul.ordersservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository
        extends JpaRepository<Order, Long> {
}
