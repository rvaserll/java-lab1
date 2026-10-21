package com.example.javalab1.product.mapper;

import com.example.javalab1.product.domain.Category;
import com.example.javalab1.product.domain.Product;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProductMapper {

    public ProductResponse toDto(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.category().id(),
                product.category().name(),
                product.stock()
        );
    }

    public Product toDomain(
            ProductRequest request,
            UUID id,
            Category category
    ) {
        return new Product(
                id,
                request.name(),
                request.description(),
                request.price(),
                category,
                request.stock()
        );
    }
}