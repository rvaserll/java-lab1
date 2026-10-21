package com.example.javalab1.order.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Order(
        UUID id,
        List<OrderItem> items,
        OrderStatus status
) {

    public Order {
        Objects.requireNonNull(id, "Order id is required.");
        Objects.requireNonNull(status, "Order status is required.");

        items = List.copyOf(items);

        if (items.isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item."
            );
        }
    }

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}