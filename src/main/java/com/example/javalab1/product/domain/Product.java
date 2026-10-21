package com.example.javalab1.product.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Category category,
        int stock
) {

    public Product {
        Objects.requireNonNull(id, "Product id is required.");
        Objects.requireNonNull(name, "Product name is required.");
        Objects.requireNonNull(price, "Product price is required.");
        Objects.requireNonNull(category, "Product category is required.");

        if (name.isBlank() || name.length() < 3 || name.length() > 120) {
            throw new IllegalArgumentException(
                    "Product name must contain 3 to 120 characters "
                            + "and must not be blank."
            );
        }

        if (description != null && description.length() > 2000) {
            throw new IllegalArgumentException(
                    "Product description must not exceed 2000 characters."
            );
        }

        if (price.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Product price must be greater than 0."
            );
        }

        if (price.compareTo(new BigDecimal("9999999999.99")) > 0
                || price.scale() > 2) {
            throw new IllegalArgumentException(
                    "Product price must have at most 10 integer digits "
                            + "and 2 fractional digits."
            );
        }

        if (stock < 0) {
            throw new IllegalArgumentException(
                    "Product stock must not be negative."
            );
        }
    }
}