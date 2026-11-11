package com.example.javalab1.product.service;

import com.example.javalab1.exception.InvalidPaginationException;
import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import com.example.javalab1.product.mapper.ProductMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class ProductServiceTest {
    private static final UUID TOYS = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FOOD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(new ProductMapper());
    }

    private ProductRequest request(String name, UUID category) {
        return new ProductRequest(name, "For space cats", new BigDecimal("12.50"), category, 3);
    }

    @Test
    void createStoresProductWithGeneratedIdAndCategoryDetails() {
        var input = request("Moon toy", TOYS);
        var created = service.create(input);
        assertThat(created.id()).isNotNull();
        assertThat(created).usingRecursiveComparison().ignoringFields("id")
                .isEqualTo(new ProductResponse(null, input.name(), input.description(), input.price(), TOYS, "Space toys", 3));
        assertThat(service.getById(created.id())).isEqualTo(created);
        assertThat(service.create(input).id()).isNotEqualTo(created.id());
    }

    @Test
    void createWithUnknownCategoryDoesNotStoreProduct() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.create(request("Moon toy", missing)))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(missing.toString());
        assertThat(service.getAll(0, 10).totalElements()).isZero();
    }

    @Test
    void getByIdReportsMissingProduct() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> service.getById(missing))
                .isInstanceOf(ResourceNotFoundException.class).hasMessageContaining(missing.toString());
    }

    @Test
    void getAllReturnsEmptyPageWithRequestedMetadata() {
        var page = service.getAll(0, 10);
        assertThat(page.content()).isEmpty();
        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isZero();
    }

    @Test
    void paginationUsesStableIdOrderWithoutLosingProducts() {
        var products = List.of(service.create(request("Moon toy", TOYS)),
                service.create(request("Star food", FOOD)), service.create(request("Mars toy", TOYS)))
                .stream().sorted(Comparator.comparing(ProductResponse::id)).toList();
        var first = service.getAll(0, 2);
        var second = service.getAll(1, 2);
        assertThat(first.content()).containsExactlyElementsOf(products.subList(0, 2));
        assertThat(second.content()).containsExactly(products.get(2));
        assertThat(first.totalElements()).isEqualTo(3);
        assertThat(second.totalElements()).isEqualTo(3);
        assertThat(second.page()).isEqualTo(1);
        assertThat(second.size()).isEqualTo(2);
        assertThat(service.getAll(2, 2).content()).isEmpty();
    }

    @Test
    void veryLargePageDoesNotOverflowOffset() {
        service.create(request("Moon toy", TOYS));
        assertThat(service.getAll(Integer.MAX_VALUE, 100).content()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"-1,10", "0,0", "0,-1", "0,101"})
    void invalidPaginationIsRejected(int page, int size) {
        assertThatThrownBy(() -> service.getAll(page, size)).isInstanceOf(InvalidPaginationException.class);
    }

    @ParameterizedTest
    @CsvSource({"0,1", "0,100"})
    void paginationAcceptsSizeBoundaries(int page, int size) {
        var created = service.create(request("Moon toy", TOYS));
        assertThat(service.getAll(page, size).content()).containsExactly(created);
    }

    @Test
    void updatePreservesIdAndReplacesAllProductFields() {
        var created = service.create(request("Moon toy", TOYS));
        var input = new ProductRequest("Star food", null, new BigDecimal("99.99"), FOOD, 0);
        var updated = service.update(created.id(), input);
        assertThat(updated).isEqualTo(new ProductResponse(created.id(), "Star food", null,
                input.price(), FOOD, "Cosmic food", 0));
        assertThat(service.getById(created.id())).isEqualTo(updated);
        assertThat(service.getAll(0, 10).totalElements()).isEqualTo(1);
    }

    @Test
    void updateMissingProductDoesNotCreateIt() {
        assertThatThrownBy(() -> service.update(UUID.randomUUID(), request("Moon toy", TOYS)))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.getAll(0, 10).content()).isEmpty();
    }

    @Test
    void updateWithUnknownCategoryKeepsOriginalProduct() {
        var original = service.create(request("Moon toy", TOYS));
        assertThatThrownBy(() -> service.update(original.id(), request("Star food", UUID.randomUUID())))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.getById(original.id())).isEqualTo(original);
    }

    @Test
    void deleteRemovesOnlyRequestedProduct() {
        var removed = service.create(request("Moon toy", TOYS));
        var retained = service.create(request("Star food", FOOD));
        service.delete(removed.id());
        assertThatThrownBy(() -> service.getById(removed.id())).isInstanceOf(ResourceNotFoundException.class);
        assertThat(service.getAll(0, 10).content()).containsExactly(retained);
        assertThatThrownBy(() -> service.delete(removed.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteMissingProductReportsNotFound() {
        assertThatThrownBy(() -> service.delete(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }
}
