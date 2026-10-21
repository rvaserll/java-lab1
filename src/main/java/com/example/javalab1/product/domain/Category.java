package com.example.javalab1.product.domain;

import java.util.Objects;
import java.util.UUID;

public record Category(
        UUID id,
        String name
) {

    public Category {
        Objects.requireNonNull(id, "Category id is required.");
        Objects.requireNonNull(name, "Category name is required.");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Category name must not be blank."
            );
        }
    }
}