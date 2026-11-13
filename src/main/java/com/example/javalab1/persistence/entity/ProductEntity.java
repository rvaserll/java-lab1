package com.example.javalab1.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "products_generator")
    @SequenceGenerator(name = "products_generator", sequenceName = "products_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    protected ProductEntity() {}
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private CategoryEntity category;
    @Column(name = "product_name", nullable = false, length = 120)
    private String name;
    @Column(length = 2000)
    private String description;
    @Column(nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal price;
    @Column(nullable = false)
    private int stock;

    public ProductEntity(com.example.javalab1.product.domain.Product product, CategoryEntity category) {
        this.publicId = product.id();
        update(product, category);
    }
    public void update(com.example.javalab1.product.domain.Product product, CategoryEntity category) {
        this.category = category;
        this.name = product.name();
        this.description = product.description();
        this.price = product.price();
        this.stock = product.stock();
    }
    public CategoryEntity getCategory() { return category; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public java.math.BigDecimal getPrice() { return price; }
    public int getStock() { return stock; }
}
