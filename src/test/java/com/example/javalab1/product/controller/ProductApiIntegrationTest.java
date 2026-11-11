package com.example.javalab1.product.controller;

import com.example.javalab1.product.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductApiIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ProductService service;

    @Test
    void productLifecycleWorksWithApplicationConfigurationAndValidation() throws Exception {
        String body = """
                {"name":"Orbit toy","description":"Integration test",
                 "price":5.50,"categoryId":"11111111-1111-1111-1111-111111111111","stock":2}
                """;
        long initialCount = service.getAll(0, 100).totalElements();
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Orbit toy", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
        assertThat(service.getAll(0, 100).totalElements()).isEqualTo(initialCount);

        var result = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(location).isNotNull();
        String path = java.net.URI.create(location).getPath();
        UUID id = UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        try {
            mvc.perform(get(path)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Orbit toy"));
            mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON)
                            .content(body.replace("Orbit toy", "Updated orbit toy")))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Updated orbit toy"));
            mvc.perform(delete(path)).andExpect(status().isNoContent());
            mvc.perform(get(path)).andExpect(status().isNotFound());
            assertThat(service.getAll(0, 100).totalElements()).isEqualTo(initialCount);
        } finally {
            if (service.getAll(0, 100).content().stream().anyMatch(product -> product.id().equals(id))) {
                service.delete(id);
            }
        }
    }
}
