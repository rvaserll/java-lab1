package com.example.javalab1.persistence.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_generator")
    @SequenceGenerator(name = "orders_generator", sequenceName = "orders_id_seq", allocationSize = 1)
    private Long id;
    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    protected OrderEntity() {}
    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.example.javalab1.order.domain.OrderStatus status;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private java.util.List<OrderItemEntity> items = new java.util.ArrayList<>();

    public OrderEntity(UUID publicId) {
        this.publicId = publicId;
        this.status = com.example.javalab1.order.domain.OrderStatus.CREATED;
    }
    public void addItem(ProductEntity product, int quantity) {
        items.add(new OrderItemEntity(this, product, quantity));
    }
    public void changeStatus(com.example.javalab1.order.domain.OrderStatus status) { this.status = status; }
    public com.example.javalab1.order.domain.OrderStatus getStatus() { return status; }
    public java.util.List<OrderItemEntity> getItems() { return java.util.Collections.unmodifiableList(items); }
}
