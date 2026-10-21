package com.example.javalab1.order.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record OrderItem(
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {

    public OrderItem {
        Objects.requireNonNull(productId, "Product id is required.");
        Objects.requireNonNull(productName, "Product name is required.");
        Objects.requireNonNull(unitPrice, "Unit price is required.");

        if (unitPrice.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Unit price must be greater than 0."
            );
        }

        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0."
            );
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}