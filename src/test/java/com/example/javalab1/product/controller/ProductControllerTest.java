package com.example.javalab1.product.controller;

import com.example.javalab1.exception.GlobalExceptionHandler;
import com.example.javalab1.product.dto.ProductRequest;
import com.example.javalab1.product.dto.ProductResponse;
import com.example.javalab1.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.json.JsonMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Exercises validation and HTTP responses against the actual application and PostgreSQL.
@org.springframework.boot.test.context.SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@org.springframework.context.annotation.Import(com.example.javalab1.support.PostgresTestConfiguration.class)
@org.springframework.transaction.annotation.Transactional
@org.springframework.security.test.context.support.WithMockUser(
        authorities = {"SCOPE_products:read", "SCOPE_products:write"})
class ProductControllerTest {
    private static final String API = "/api/products";
    private static final UUID TOYS = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private final JsonMapper json = JsonMapper.builder().build();
    @org.springframework.beans.factory.annotation.Autowired
    private ProductService service;
    private ProductResponse original;
    @org.springframework.beans.factory.annotation.Autowired
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        original = service.create(new ProductRequest("Original toy", "Original description",
                new BigDecimal("10.00"), TOYS, 5));
    }

    private MockMvc mvcFor(ProductService productService) {
        return MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    private static Map<String, Object> validBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Moon toy");
        body.put("description", "For space cats");
        body.put("price", new BigDecimal("12.50"));
        body.put("categoryId", TOYS.toString());
        body.put("stock", 3);
        return body;
    }

    private String path(String method) {
        return method.equals("POST") ? API : API + "/" + original.id();
    }

    @Test
    void createReturns201LocationAndPersistedProduct() throws Exception {
        var result = mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Moon toy"))
                .andExpect(jsonPath("$.categoryName").value("Space toys"))
                .andExpect(jsonPath("$.stock").value(3))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(location).startsWith("http://localhost/api/products/");
        String id = location.substring(location.lastIndexOf('/') + 1);
        mvc.perform(get(API + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.price").value(12.50));
    }

    @Test
    void updateReturnsChangedProductWithOriginalId() throws Exception {
        mvc.perform(put(path("PUT")).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(original.id().toString()))
                .andExpect(jsonPath("$.name").value("Moon toy"));
        assertThat(service.getById(original.id()).name()).isEqualTo("Moon toy");
    }

    private record InvalidField(String field, Object value) {}

    static Stream<Arguments> invalidFields() {
        var invalid = List.of(
                new InvalidField("name", null), new InvalidField("name", ""),
                new InvalidField("name", "   "), new InvalidField("name", "ab"),
                new InvalidField("name", "x".repeat(121)),
                new InvalidField("description", "x".repeat(2001)),
                new InvalidField("price", null), new InvalidField("price", 0),
                new InvalidField("price", -1), new InvalidField("price", new BigDecimal("0.001")),
                new InvalidField("price", new BigDecimal("10000000000.00")),
                new InvalidField("categoryId", null), new InvalidField("stock", null),
                new InvalidField("stock", -1));
        return Stream.of("POST", "PUT").flatMap(method -> invalid.stream()
                .map(test -> Arguments.of(method, test.field(), test.value())));
    }

    @ParameterizedTest(name = "{0}: invalid {1} = {2}")
    @MethodSource("invalidFields")
    void invalidFieldsReturn400WithoutChangingStorage(String method, String field, Object value) throws Exception {
        var body = validBody();
        body.put(field, value);
        mvc.perform(request(HttpMethod.valueOf(method), path(method))
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation"))
                .andExpect(jsonPath("$.instance").value(path(method)))
                .andExpect(jsonPath("$.errors[*].field", hasItem(field)));
        assertThat(service.getAll(0, 10).content()).containsExactly(original);
    }

    static Stream<Arguments> validBoundaries() {
        return Stream.of("POST", "PUT").flatMap(method -> Stream.of(
                Arguments.of(method, "abc", null, "0.01", 0),
                Arguments.of(method, "x".repeat(120), "x".repeat(2000), "9999999999.99", Integer.MAX_VALUE)));
    }

    @ParameterizedTest
    @MethodSource("validBoundaries")
    void acceptsValidBoundaryValues(String method, String name, String description, String price, int stock) throws Exception {
        var body = validBody();
        body.put("name", name);
        body.put("description", description);
        body.put("price", new BigDecimal(price));
        body.put("stock", stock);
        mvc.perform(request(HttpMethod.valueOf(method), path(method)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().is(method.equals("POST") ? 201 : 200))
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.stock").value(stock));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void unknownCategoryReturns404AndKeepsStorage(String method) throws Exception {
        var body = validBody();
        body.put("categoryId", UUID.randomUUID().toString());
        mvc.perform(request(HttpMethod.valueOf(method), path(method)).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"));
        assertThat(service.getAll(0, 10).content()).containsExactly(original);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void missingProductReturns404(String method) throws Exception {
        String path = API + "/" + UUID.randomUUID();
        mvc.perform(request(HttpMethod.valueOf(method), path).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(validBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value(path));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void malformedProductIdReturns400(String method) throws Exception {
        mvc.perform(request(HttpMethod.valueOf(method), API + "/not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(validBody())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{", "", "{\"stock\": \"abc\"}"})
    void malformedOrMissingJsonReturns400(String body) throws Exception {
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "page=abc", "size=abc"})
    void invalidPaginationReturns400(String query) throws Exception {
        mvc.perform(get(API + "?" + query)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listUsesDefaultPaginationAndReturnsMetadata() throws Exception {
        mvc.perform(get(API)).andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(original.id().toString()));
    }

    @Test
    void deleteReturns204AndProductIsNoLongerAccessible() throws Exception {
        mvc.perform(delete(API + "/" + original.id())).andExpect(status().isNoContent())
                .andExpect(content().string(""));
        mvc.perform(get(API + "/" + original.id())).andExpect(status().isNotFound());
    }

    @Test
    void duplicateNameInSameCategoryReturns409WithoutExposingSql() throws Exception {
        var body = validBody();
        body.put("name", original.name());
        mvc.perform(post(API).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/conflict"))
                .andExpect(content().string(not(containsString("uk_products_category_name"))));
    }

    @Test
    void referencedProductDeletionReturns409() throws Exception {
        var conflictService = mock(ProductService.class);
        doThrow(new com.example.javalab1.exception.ResourceConflictException("Product is referenced by an order."))
                .when(conflictService).delete(original.id());
        mvcFor(conflictService).perform(delete(API + "/" + original.id()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource conflict"));
    }

    @Test
    void unexpectedFailureReturns500WithoutLeakingInternalDetails() throws Exception {
        var failingService = mock(ProductService.class);
        when(failingService.getById(original.id())).thenThrow(new IllegalStateException("private database password"));
        mvcFor(failingService).perform(get(API + "/" + original.id()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(content().string(not(containsString("private database password"))));
        verify(failingService).getById(original.id());
    }
}
