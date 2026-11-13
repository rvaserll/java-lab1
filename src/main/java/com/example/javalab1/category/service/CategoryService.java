package com.example.javalab1.category.service;

import com.example.javalab1.exception.ResourceConflictException;
import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.persistence.MarketPersistenceMapper;
import com.example.javalab1.persistence.entity.CategoryEntity;
import com.example.javalab1.persistence.repository.CategoryRepository;
import com.example.javalab1.persistence.repository.ProductRepository;
import com.example.javalab1.product.domain.Category;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final MarketPersistenceMapper mapper;

    public CategoryService(CategoryRepository categories, ProductRepository products, MarketPersistenceMapper mapper) {
        this.categories = categories;
        this.products = products;
        this.mapper = mapper;
    }
    @Transactional
    public Category create(String name) {
        var category = new Category(UUID.randomUUID(), name);
        return mapper.category(categories.saveAndFlush(new CategoryEntity(category.id(), category.name())));
    }
    public Category getById(UUID id) { return mapper.category(find(id)); }
    public List<Category> getAll() {
        return categories.findAll(Sort.by("name")).stream().map(mapper::category).toList();
    }
    @Transactional
    public Category update(UUID id, String name) {
        var validated = new Category(id, name);
        var entity = find(id);
        entity.rename(validated.name());
        categories.flush();
        return mapper.category(entity);
    }
    @Transactional
    public void delete(UUID id) {
        var category = find(id);
        if (products.existsByCategoryId(category.getId())) {
            throw new ResourceConflictException("Category contains products.");
        }
        categories.delete(category);
        categories.flush();
    }
    private CategoryEntity find(UUID id) {
        return categories.findByPublicId(id).orElseThrow(() ->
                new ResourceNotFoundException("Category with id " + id + " was not found."));
    }
}
