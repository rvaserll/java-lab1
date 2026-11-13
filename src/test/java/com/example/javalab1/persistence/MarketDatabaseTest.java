package com.example.javalab1.persistence;

import com.example.javalab1.category.service.CategoryService;
import com.example.javalab1.exception.ResourceConflictException;
import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.order.domain.OrderStatus;
import com.example.javalab1.order.dto.OrderLineRequest;
import com.example.javalab1.order.service.OrderService;
import com.example.javalab1.persistence.entity.*;
import com.example.javalab1.persistence.repository.*;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import com.example.javalab1.product.service.ProductService;
import com.example.javalab1.support.DatabaseTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;

class MarketDatabaseTest extends DatabaseTestSupport {
    private static final UUID TOYS = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FOOD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    @Autowired CategoryService categories;
    @Autowired ProductService products;
    @Autowired OrderService orders;
    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired OrderRepository orderRepository;
    @Autowired OrderItemRepository itemRepository;
    @Autowired EntityManager entityManager;
    @Autowired JdbcTemplate jdbc;

    private ProductRequest request(String name, UUID category) {
        return new ProductRequest(name, "Description", new BigDecimal("5.50"), category, 10);
    }
    private ProductResponse product(String name) { return products.create(request(name, TOYS)); }

    @Test
    void categoryCrudPersistsChangesAndPreservesPublicId() {
        var created = categories.create("Accessories");
        entityManager.clear();
        assertThat(categories.getById(created.id())).isEqualTo(created);
        var updated = categories.update(created.id(), "Space accessories");
        entityManager.clear();
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(categories.getById(created.id()).name()).isEqualTo("Space accessories");
        assertThat(categories.getAll()).extracting("name").isSorted();
        categories.delete(created.id());
        assertThatThrownBy(() -> categories.getById(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void categoryWithProductsCannotBeDeleted() {
        product("Moon toy");
        assertThatThrownBy(() -> categories.delete(TOYS)).isInstanceOf(ResourceConflictException.class);
        assertThat(categoryRepository.findByPublicId(TOYS)).isPresent();
    }

    @Test
    void unknownCategoryOperationsFail() {
        UUID id = UUID.randomUUID();
        assertThatThrownBy(() -> categories.getById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> categories.update(id, "Unknown category")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> categories.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void orderCrudAndCascadeDeleteWorkAfterReload() {
        var first = product("Moon toy");
        var second = product("Mars toy");
        var created = orders.create(List.of(new OrderLineRequest(first.id(), 2), new OrderLineRequest(second.id(), 3)));
        entityManager.clear();
        var loaded = orders.getById(created.id());
        assertThat(loaded).isEqualTo(created);
        assertThat(loaded.total()).isEqualByComparingTo("27.50");
        assertThat(orders.getAll()).containsExactly(loaded);
        assertThat(orders.updateStatus(created.id(), OrderStatus.PAID).status()).isEqualTo(OrderStatus.PAID);
        entityManager.clear();
        assertThat(orders.getById(created.id()).status()).isEqualTo(OrderStatus.PAID);
        assertThat(itemRepository.findAll()).hasSize(2).allSatisfy(item -> assertThat(item.getId()).isPositive());
        assertThat(orderRepository.findByPublicId(created.id()).orElseThrow().getId()).isPositive();
        orders.delete(created.id());
        entityManager.clear();
        assertThat(itemRepository.count()).isZero();
        assertThat(orderRepository.count()).isZero();
        assertThat(productRepository.count()).isEqualTo(2);
        assertThatThrownBy(() -> orders.getById(created.id())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void orderKeepsHistoricalNameAndPriceAfterProductUpdate() {
        var product = product("Original toy");
        var order = orders.create(List.of(new OrderLineRequest(product.id(), 2)));
        products.update(product.id(), new ProductRequest("Changed toy", null, new BigDecimal("99.00"), FOOD, 1));
        entityManager.clear();
        var loaded = orders.getById(order.id());
        assertThat(loaded.items().get(0).productName()).isEqualTo("Original toy");
        assertThat(loaded.items().get(0).unitPrice()).isEqualByComparingTo("5.50");
        assertThat(loaded.total()).isEqualByComparingTo("11.00");
    }

    @Test
    void orderedProductCannotBeDeleted() {
        var product = product("Ordered toy");
        orders.create(List.of(new OrderLineRequest(product.id(), 1)));
        assertThatThrownBy(() -> products.delete(product.id())).isInstanceOf(ResourceConflictException.class);
    }

    @Test
    void emptyOrderAndInvalidLinesAreRejected() {
        assertThatThrownBy(() -> orders.create(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> orders.create(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new OrderLineRequest(TOYS, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderLineRequest(null, 1)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void unknownOrderAndNullStatusAreRejected() {
        var id = UUID.randomUUID();
        assertThatThrownBy(() -> orders.getById(id)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.updateStatus(id, OrderStatus.PAID)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> orders.updateStatus(id, null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> orders.delete(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void missingSecondProductRollsBackAlreadyInsertedOrderHeader() {
        var product = product("Rollback toy " + UUID.randomUUID());
        long before = orderRepository.count();
        long itemsBefore = itemRepository.count();
        try {
            assertThatThrownBy(() -> orders.create(List.of(new OrderLineRequest(product.id(), 1),
                    new OrderLineRequest(UUID.randomUUID(), 1)))).isInstanceOf(ResourceNotFoundException.class);
            assertThat(orderRepository.count()).isEqualTo(before);
            assertThat(itemRepository.count()).isEqualTo(itemsBefore);
        } finally {
            products.delete(product.id());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void categoryNameIsUniqueInDatabase() {
        String name = "Unique category " + UUID.randomUUID();
        var category = categories.create(name);
        try {
            assertThatThrownBy(() -> categories.create(name)).isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            categories.delete(category.id());
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void productNameIsUniqueOnlyWithinItsCategoryAndFailedUpdateRollsBack() {
        String name = "Unique toy " + UUID.randomUUID();
        var first = products.create(request(name, TOYS));
        var other = products.create(request(name, FOOD));
        var third = products.create(request("Other " + name, TOYS));
        try {
            assertThatThrownBy(() -> products.create(request(name, TOYS))).isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> products.update(third.id(), request(name, TOYS))).isInstanceOf(DataIntegrityViolationException.class);
            assertThat(products.getById(third.id()).name()).isEqualTo(third.name());
            assertThat(products.getById(other.id()).categoryId()).isEqualTo(FOOD);
        } finally {
            products.delete(first.id()); products.delete(other.id()); products.delete(third.id());
        }
    }

    @Test
    void migrationsAreTrackedAndIndexesAndSequencesExist() {
        assertThat(jdbc.queryForObject("select count(*) from databasechangelog", Integer.class)).isEqualTo(5);
        assertThat(jdbc.queryForList("select sequencename from pg_sequences where schemaname='public'", String.class))
                .contains("categories_id_seq", "products_id_seq", "orders_id_seq", "order_items_id_seq");
        assertThat(jdbc.queryForList("select indexname from pg_indexes where schemaname='public'", String.class))
                .contains("idx_products_name", "idx_order_items_order", "idx_order_items_product", "uk_products_category_name", "uk_categories_name");
        var category = categories.create("Sequence category");
        var product = products.create(request("Sequence toy", category.id()));
        entityManager.clear();
        assertThat(categoryRepository.findByPublicId(category.id()).orElseThrow().getId()).isGreaterThan(2L);
        assertThat(productRepository.findByPublicId(product.id()).orElseThrow().getId()).isPositive();
    }

    @Test
    void databaseRejectsDanglingCategoryReference() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into products(public_id, category_id, product_name, price, stock)
                values (?, -1, 'Invalid reference', 1, 0)
                """, UUID.randomUUID())).isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"price=0", "price=-1", "stock=-1", "product_name='  '", "product_name='ab'"})
    void databaseRejectsInvalidProductValues(String assignment) {
        var product = product("Constraint toy");
        assertThatThrownBy(() -> jdbc.update("update products set " + assignment + " where public_id=?", product.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRestrictsDeletingReferencedCategory() {
        product("Referenced toy");
        assertThatThrownBy(() -> jdbc.update("delete from categories where public_id=?", TOYS))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRestrictsDeletingOrderedProduct() {
        var product = product("Referenced product");
        orders.create(List.of(new OrderLineRequest(product.id(), 1)));
        assertThatThrownBy(() -> jdbc.update("delete from products where public_id=?", product.id()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseCascadesOrderDeletionToItsItems() {
        var product = product("Cascade toy");
        var order = orders.create(List.of(new OrderLineRequest(product.id(), 1)));
        entityManager.clear();
        jdbc.update("delete from orders where public_id=?", order.id());
        assertThat(itemRepository.count()).isZero();
        assertThat(productRepository.findByPublicId(product.id())).isPresent();
    }

    @Test
    void paginationPreservesJavaUuidOrderAcrossSignedHalves() {
        var category = categoryRepository.findByPublicId(TOYS).orElseThrow();
        List<UUID> ids = List.of(UUID.fromString("80000000-0000-0000-0000-000000000000"),
                UUID.fromString("00000000-0000-0000-8000-000000000000"),
                UUID.fromString("00000000-0000-0000-0000-000000000000"),
                UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
        for (int i=0; i<ids.size(); i++) {
            var domain = new com.example.javalab1.product.domain.Product(ids.get(i), "Ordered " + i,
                    null, BigDecimal.ONE, new com.example.javalab1.product.domain.Category(TOYS, "Space toys"), 0);
            productRepository.save(new ProductEntity(domain, category));
        }
        productRepository.flush();
        entityManager.clear();
        assertThat(products.getAll(0, 2).content()).extracting(ProductResponse::id)
                .containsExactlyElementsOf(ids.stream().sorted().limit(2).toList());
        assertThat(products.getAll(1, 2).content()).extracting(ProductResponse::id)
                .containsExactlyElementsOf(ids.stream().sorted().skip(2).toList());
    }
}
