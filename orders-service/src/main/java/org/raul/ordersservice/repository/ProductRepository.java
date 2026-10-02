package org.raul.ordersservice.repository;

import org.raul.ordersservice.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository
        extends JpaRepository<Product, Long> {
}
