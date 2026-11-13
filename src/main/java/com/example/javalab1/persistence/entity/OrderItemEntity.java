package com.example.javalab1.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_items_generator")
    @SequenceGenerator(name = "order_items_generator", sequenceName = "order_items_id_seq", allocationSize = 1)
    private Long id;

    protected OrderItemEntity() {}
    public Long getId() { return id; }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;
    @Column(name = "product_name_at_purchase", nullable = false, length = 120)
    private String productName;
    @Column(name = "unit_price_at_purchase", nullable = false, precision = 12, scale = 2)
    private java.math.BigDecimal unitPrice;
    @Column(nullable = false)
    private int quantity;

    public OrderItemEntity(OrderEntity order, ProductEntity product, int quantity) {
        new com.example.javalab1.order.domain.OrderItem(product.getPublicId(), product.getName(), product.getPrice(), quantity);
        this.order = order;
        this.product = product;
        this.productName = product.getName();
        this.unitPrice = product.getPrice();
        this.quantity = quantity;
    }
    public ProductEntity getProduct() { return product; }
    public String getProductName() { return productName; }
    public java.math.BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
