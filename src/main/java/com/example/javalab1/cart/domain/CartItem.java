package com.example.javalab1.cart.domain;

import com.example.javalab1.product.domain.Product;

import java.math.BigDecimal;
import java.util.Objects;

public record CartItem(
        Product product,
        int quantity
) {

    public CartItem {
        Objects.requireNonNull(product, "Product is required.");

        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than 0."
            );
        }
    }

    public BigDecimal subtotal() {
        return product.price().multiply(
                BigDecimal.valueOf(quantity)
        );
    }
}