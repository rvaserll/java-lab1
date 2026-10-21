package com.example.javalab1.product.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        Category category,
        int stock
) {
}