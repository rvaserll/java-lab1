package com.example.javalab1.product.domain;

import java.util.UUID;

public record Category(
        UUID id,
        String name
) {
}