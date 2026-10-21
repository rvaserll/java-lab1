package com.example.javalab1.cart.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record Cart(
        UUID id,
        List<CartItem> items
) {

    public Cart {
        Objects.requireNonNull(id, "Cart id is required.");
        items = List.copyOf(items);
    }

    public BigDecimal total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}