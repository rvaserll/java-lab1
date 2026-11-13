package com.example.javalab1.support;

import com.example.javalab1.category.service.CategoryService;
import com.example.javalab1.order.service.OrderService;
import com.example.javalab1.persistence.MarketPersistenceMapper;
import com.example.javalab1.product.mapper.ProductMapper;
import com.example.javalab1.product.service.ProductService;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = {"spring.test.database.replace=none", "spring.jpa.show-sql=false"})
@Import({PostgresTestConfiguration.class, ProductService.class, CategoryService.class,
        OrderService.class, ProductMapper.class, MarketPersistenceMapper.class})
public abstract class DatabaseTestSupport {}
