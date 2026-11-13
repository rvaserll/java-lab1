package com.example.javalab1.order.service;

import com.example.javalab1.exception.ResourceNotFoundException;
import com.example.javalab1.order.domain.Order;
import com.example.javalab1.order.domain.OrderStatus;
import com.example.javalab1.order.dto.OrderLineRequest;
import com.example.javalab1.persistence.MarketPersistenceMapper;
import com.example.javalab1.persistence.entity.OrderEntity;
import com.example.javalab1.persistence.repository.OrderRepository;
import com.example.javalab1.persistence.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final MarketPersistenceMapper mapper;

    public OrderService(OrderRepository orders, ProductRepository products, MarketPersistenceMapper mapper) {
        this.orders = orders;
        this.products = products;
        this.mapper = mapper;
    }
    @Transactional
    public Order create(List<OrderLineRequest> lines) {
        Objects.requireNonNull(lines, "Order items are required.");
        if (lines.isEmpty()) throw new IllegalArgumentException("Order must contain at least one item.");
        var order = orders.saveAndFlush(new OrderEntity(UUID.randomUUID()));
        for (var line : lines) {
            var product = products.findByPublicId(line.productId()).orElseThrow(() ->
                    new ResourceNotFoundException("Product with id " + line.productId() + " was not found."));
            order.addItem(product, line.quantity());
        }
        orders.flush();
        return mapper.order(order);
    }
    public Order getById(UUID id) { return mapper.order(find(id)); }
    public List<Order> getAll() { return orders.findAll().stream().map(mapper::order).toList(); }
    @Transactional
    public Order updateStatus(UUID id, OrderStatus status) {
        Objects.requireNonNull(status, "Order status is required.");
        var order = find(id);
        order.changeStatus(status);
        orders.flush();
        return mapper.order(order);
    }
    @Transactional
    public void delete(UUID id) {
        orders.delete(find(id));
        orders.flush();
    }
    private OrderEntity find(UUID id) {
        return orders.findByPublicId(id).orElseThrow(() ->
                new ResourceNotFoundException("Order with id " + id + " was not found."));
    }
}
