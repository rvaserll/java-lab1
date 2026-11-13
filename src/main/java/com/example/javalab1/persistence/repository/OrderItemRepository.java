package com.example.javalab1.persistence.repository;

import com.example.javalab1.persistence.entity.OrderItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {
    boolean existsByProductId(Long productId);
}
