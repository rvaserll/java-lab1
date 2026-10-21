package com.example.javalab1.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
        @NotBlank
        @Size(min = 3, max = 120)
        String name,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin(value = "0", inclusive = false)
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull
        UUID categoryId,

        @NotNull
        @PositiveOrZero
        Integer stock
) {
}