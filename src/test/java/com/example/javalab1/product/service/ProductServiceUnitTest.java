package com.example.javalab1.product.service;

import com.example.javalab1.exception.ResourceConflictException;
import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.persistence.MarketPersistenceMapper;
import com.example.javalab1.persistence.entity.CategoryEntity;
import com.example.javalab1.persistence.entity.ProductEntity;
import com.example.javalab1.persistence.repository.*;
import com.example.javalab1.product.domain.Category;
import com.example.javalab1.product.domain.Product;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceUnitTest {
    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    @Mock OrderItemRepository items;
    private ProductService service;
    private final UUID categoryId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProductService(products, categories, items, new ProductMapper(), new MarketPersistenceMapper());
    }
    private ProductRequest request() {
        return new ProductRequest("Moon toy", null, new BigDecimal("5.50"), categoryId, 3);
    }
    private ProductEntity product() {
        var category = new CategoryEntity(categoryId, "Toys");
        return new ProductEntity(new Product(productId, "Old toy", null, BigDecimal.ONE,
                new Category(categoryId, "Toys"), 1), category);
    }

    @Test
    void createReturnsGeneratedPublicIdAndPersistsRequestedFields() {
        when(categories.findByPublicId(categoryId)).thenReturn(Optional.of(new CategoryEntity(categoryId, "Toys")));
        when(products.saveAndFlush(any(ProductEntity.class))).thenAnswer(call -> call.getArgument(0));
        var created = service.create(request());
        assertThat(created.id()).isNotNull();
        assertThat(created.name()).isEqualTo("Moon toy");
        assertThat(created.price()).isEqualByComparingTo("5.50");
        assertThat(created.categoryId()).isEqualTo(categoryId);
        verify(products).saveAndFlush(argThat(entity -> entity.getPublicId().equals(created.id()) && entity.getStock() == 3));
    }

    @Test
    void missingCategoryPreventsAnyProductWrite() {
        when(categories.findByPublicId(categoryId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(request())).isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(products, items);
    }

    @Test
    void readUsesPublicUuidRatherThanInternalNumericId() {
        when(products.findByPublicId(productId)).thenReturn(Optional.of(product()));
        assertThat(service.getById(productId).id()).isEqualTo(productId);
        verify(products).findByPublicId(productId);
        verify(products, never()).findById(anyLong());
    }

    @Test
    void hugeEmptyPageDoesNotPassAnOverflowingOffsetToJpa() {
        when(products.count()).thenReturn(1L);
        var page = service.getAll(Integer.MAX_VALUE, 100);
        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isEqualTo(1);
        verify(products, never()).findInApiOrder(any());
    }

    @Test
    void updatePreservesPublicIdAndFlushesChanges() {
        var entity = product();
        when(categories.findByPublicId(categoryId)).thenReturn(Optional.of(new CategoryEntity(categoryId, "Toys")));
        when(products.findByPublicId(productId)).thenReturn(Optional.of(entity));
        var changed = service.update(productId, request());
        assertThat(changed.id()).isEqualTo(productId);
        assertThat(entity.getName()).isEqualTo("Moon toy");
        verify(products).flush();
    }

    @Test
    void orderedProductIsNeverPassedToDelete() {
        var entity = mock(ProductEntity.class);
        when(entity.getId()).thenReturn(42L);
        when(products.findByPublicId(productId)).thenReturn(Optional.of(entity));
        when(items.existsByProductId(42L)).thenReturn(true);
        assertThatThrownBy(() -> service.delete(productId)).isInstanceOf(ResourceConflictException.class);
        verify(products, never()).delete(any(ProductEntity.class));
    }

    @Test
    void unreferencedProductIsDeletedAndFlushed() {
        var entity = mock(ProductEntity.class);
        when(entity.getId()).thenReturn(42L);
        when(products.findByPublicId(productId)).thenReturn(Optional.of(entity));
        service.delete(productId);
        verify(products).delete(entity);
        verify(products).flush();
    }
}
