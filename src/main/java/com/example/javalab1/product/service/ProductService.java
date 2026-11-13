package com.example.javalab1.product.service;

import com.example.javalab1.exception.*;
import com.example.javalab1.persistence.MarketPersistenceMapper;
import com.example.javalab1.persistence.entity.CategoryEntity;
import com.example.javalab1.persistence.entity.ProductEntity;
import com.example.javalab1.persistence.repository.*;
import com.example.javalab1.product.dto.*;
import com.example.javalab1.product.mapper.ProductMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final OrderItemRepository orderItems;
    private final ProductMapper mapper;
    private final MarketPersistenceMapper persistenceMapper;

    public ProductService(ProductRepository products, CategoryRepository categories,
                          OrderItemRepository orderItems, ProductMapper mapper,
                          MarketPersistenceMapper persistenceMapper) {
        this.products = products;
        this.categories = categories;
        this.orderItems = orderItems;
        this.mapper = mapper;
        this.persistenceMapper = persistenceMapper;
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        CategoryEntity category = findCategory(request.categoryId());
        var domain = mapper.toDomain(request, UUID.randomUUID(), persistenceMapper.category(category));
        var product = products.saveAndFlush(new ProductEntity(domain, category));
        return mapper.toDto(persistenceMapper.product(product));
    }

    public ProductResponse getById(UUID id) {
        return mapper.toDto(persistenceMapper.product(findProduct(id)));
    }

    public ProductPageResponse getAll(int page, int size) {
        if (page < 0) throw new InvalidPaginationException("Page must be greater than or equal to 0.");
        if (size < 1 || size > 100) throw new InvalidPaginationException("Size must be between 1 and 100.");
        long total = products.count();
        if ((long) page * size >= total) return new ProductPageResponse(List.of(), page, size, total);
        var content = products.findInApiOrder(PageRequest.of(page, size)).stream()
                .map(persistenceMapper::product).map(mapper::toDto).toList();
        return new ProductPageResponse(content, page, size, total);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        CategoryEntity category = findCategory(request.categoryId());
        ProductEntity product = findProduct(id);
        product.update(mapper.toDomain(request, id, persistenceMapper.category(category)), category);
        products.flush();
        return mapper.toDto(persistenceMapper.product(product));
    }

    @Transactional
    public void delete(UUID id) {
        ProductEntity product = findProduct(id);
        if (orderItems.existsByProductId(product.getId())) {
            throw new ResourceConflictException("Product is referenced by an order.");
        }
        products.delete(product);
        products.flush();
    }

    private CategoryEntity findCategory(UUID id) {
        return categories.findByPublicId(id).orElseThrow(() ->
                new ResourceNotFoundException("Category with id " + id + " was not found."));
    }
    private ProductEntity findProduct(UUID id) {
        return products.findByPublicId(id).orElseThrow(() ->
                new ResourceNotFoundException("Product with id " + id + " was not found."));
    }
}
