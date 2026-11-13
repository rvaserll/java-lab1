package com.example.javalab1.persistence;

import com.example.javalab1.persistence.entity.*;
import com.example.javalab1.product.domain.Category;
import com.example.javalab1.product.domain.Product;
import com.example.javalab1.order.domain.Order;
import com.example.javalab1.order.domain.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class MarketPersistenceMapper {
    public Category category(CategoryEntity entity) {
        return new Category(entity.getPublicId(), entity.getName());
    }
    public Product product(ProductEntity entity) {
        return new Product(entity.getPublicId(), entity.getName(), entity.getDescription(),
                entity.getPrice(), category(entity.getCategory()), entity.getStock());
    }
    public Order order(OrderEntity entity) {
        return new Order(entity.getPublicId(), entity.getItems().stream()
                .map(item -> new OrderItem(item.getProduct().getPublicId(), item.getProductName(),
                        item.getUnitPrice(), item.getQuantity())).toList(), entity.getStatus());
    }
}
