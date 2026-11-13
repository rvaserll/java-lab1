package com.example.javalab1.persistence.repository;

import com.example.javalab1.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
    @Override
    @EntityGraph(attributePaths = {"items", "items.product"})
    java.util.List<OrderEntity> findAll();

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<OrderEntity> findByPublicId(UUID publicId);
}
