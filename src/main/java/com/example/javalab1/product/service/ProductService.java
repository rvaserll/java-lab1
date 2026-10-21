package com.example.javalab1.product.service;

import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.product.domain.Category;
import com.example.javalab1.product.domain.Product;
import com.example.javalab1.product.dto.ProductPageResponse;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import com.example.javalab1.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ProductService {

    private final ConcurrentMap<UUID, Product> products =
            new ConcurrentHashMap<>();

    private final Map<UUID, Category> categories;
    private final ProductMapper mapper;

    public ProductService(ProductMapper mapper) {
        this.mapper = mapper;

        UUID toysId = UUID.fromString(
                "11111111-1111-1111-1111-111111111111"
        );
        UUID foodId = UUID.fromString(
                "22222222-2222-2222-2222-222222222222"
        );

        this.categories = Map.of(
                toysId, new Category(toysId, "Space toys"),
                foodId, new Category(foodId, "Cosmic food")
        );
    }

    public ProductResponse create(ProductRequest request) {
        Category category = findCategory(request.categoryId());
        UUID id = UUID.randomUUID();

        Product product = mapper.toDomain(request, id, category);
        products.put(id, product);

        return mapper.toDto(product);
    }

    public ProductResponse getById(UUID id) {
        Product product = products.get(id);

        if (product == null) {
            throw productNotFound(id);
        }

        return mapper.toDto(product);
    }

    public ProductPageResponse getAll(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0."
            );
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Size must be between 1 and 100."
            );
        }

        List<Product> snapshot = products.values().stream()
                .sorted(Comparator.comparing(Product::id))
                .toList();

        long offset = (long) page * size;

        List<ProductResponse> content = snapshot.stream()
                .skip(offset)
                .limit(size)
                .map(mapper::toDto)
                .toList();

        return new ProductPageResponse(
                content,
                page,
                size,
                snapshot.size()
        );
    }

    public ProductResponse update(UUID id, ProductRequest request) {
        Category category = findCategory(request.categoryId());

        Product updated = products.computeIfPresent(
                id,
                (key, existing) -> mapper.toDomain(
                        request,
                        key,
                        category
                )
        );

        if (updated == null) {
            throw productNotFound(id);
        }

        return mapper.toDto(updated);
    }

    public void delete(UUID id) {
        Product removed = products.remove(id);

        if (removed == null) {
            throw productNotFound(id);
        }
    }

    private Category findCategory(UUID id) {
        Category category = categories.get(id);

        if (category == null) {
            throw new ResourceNotFoundException(
                    "Category with id " + id + " was not found."
            );
        }

        return category;
    }

    private ResourceNotFoundException productNotFound(UUID id) {
        return new ResourceNotFoundException(
                "Product with id " + id + " was not found."
        );
    }
}