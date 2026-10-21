package com.example.javalab1.product.controller;

import com.example.javalab1.product.dto.ProductPageResponse;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import com.example.javalab1.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse product = service.create(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(product.id())
                .toUri();

        return ResponseEntity.created(location).body(product);
    }

    @GetMapping
    public ProductPageResponse getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getAll(page, size);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return service.getById(id);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}