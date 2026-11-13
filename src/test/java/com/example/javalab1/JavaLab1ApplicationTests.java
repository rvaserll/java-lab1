package com.example.javalab1;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@org.springframework.context.annotation.Import(com.example.javalab1.support.PostgresTestConfiguration.class)
@org.springframework.transaction.annotation.Transactional
class JavaLab1ApplicationTests {

    @Test
    void contextLoads() {
    }

}
