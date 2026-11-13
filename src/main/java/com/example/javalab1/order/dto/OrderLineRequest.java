package com.example.javalab1.order.dto;

import java.util.Objects;
import java.util.UUID;

public record OrderLineRequest(UUID productId, int quantity) {
    public OrderLineRequest {
        Objects.requireNonNull(productId, "Product id is required.");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be greater than 0.");
    }
}
