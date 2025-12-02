package tqs.blacktie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tqs.blacktie.entity.Product;
import tqs.blacktie.service.ProductService;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Autowired
    private ObjectMapper objectMapper;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product("Smoking", "Premium smoking suit", 80.0);
        testProduct.setId(1L);
        testProduct.setAvailable(true);
    }

    @Nested
    @DisplayName("GET /api/products")
    class GetProductsTests {

        @Test
        @DisplayName("Should return all available products without filters")
        void whenGetProductsNoFilters_thenReturnAll() throws Exception {
            Product product2 = new Product("Tuxedo", "Classic tuxedo", 120.0);
            product2.setId(2L);
            product2.setAvailable(true);

            when(productService.getAvailableProducts(isNull(), isNull()))
                .thenReturn(Arrays.asList(testProduct, product2));

            mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Smoking"))
                .andExpect(jsonPath("$[0].price").value(80.0))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Tuxedo"));
        }

        @Test
        @DisplayName("Should filter products by name")
        void whenGetProductsWithNameFilter_thenReturnFiltered() throws Exception {
            when(productService.getAvailableProducts(eq("smoking"), isNull()))
                .thenReturn(List.of(testProduct));

            mockMvc.perform(get("/api/products")
                    .param("name", "smoking"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Smoking"));
        }

        @Test
        @DisplayName("Should filter products by max price")
        void whenGetProductsWithMaxPriceFilter_thenReturnFiltered() throws Exception {
            when(productService.getAvailableProducts(isNull(), eq(100.0)))
                .thenReturn(List.of(testProduct));

            mockMvc.perform(get("/api/products")
                    .param("maxPrice", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].price").value(80.0));
        }

        @Test
        @DisplayName("Should filter products by name and max price")
        void whenGetProductsWithBothFilters_thenReturnFiltered() throws Exception {
            when(productService.getAvailableProducts(eq("smoking"), eq(100.0)))
                .thenReturn(List.of(testProduct));

            mockMvc.perform(get("/api/products")
                    .param("name", "smoking")
                    .param("maxPrice", "100.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Smoking"));
        }

        @Test
        @DisplayName("Should return empty list when no products match")
        void whenGetProductsNoMatch_thenReturnEmptyList() throws Exception {
            when(productService.getAvailableProducts(eq("nonexistent"), isNull()))
                .thenReturn(List.of());

            mockMvc.perform(get("/api/products")
                    .param("name", "nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/products")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void whenCreateProduct_thenReturn201() throws Exception {
            Product newProduct = new Product("New Suit", "A new suit", 150.0);
            
            Product savedProduct = new Product("New Suit", "A new suit", 150.0);
            savedProduct.setId(1L);
            savedProduct.setAvailable(true);
            
            when(productService.createProduct(any(Product.class))).thenReturn(savedProduct);

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New Suit"))
                .andExpect(jsonPath("$.description").value("A new suit"))
                .andExpect(jsonPath("$.price").value(150.0))
                .andExpect(jsonPath("$.available").value(true));
        }

        @Test
        @DisplayName("Should create product with explicit availability")
        void whenCreateProductWithAvailability_thenReturn201() throws Exception {
            Product newProduct = new Product("New Suit", "A new suit", 150.0);
            newProduct.setAvailable(false);
            
            Product savedProduct = new Product("New Suit", "A new suit", 150.0);
            savedProduct.setId(1L);
            savedProduct.setAvailable(false);
            
            when(productService.createProduct(any(Product.class))).thenReturn(savedProduct);

            mockMvc.perform(post("/api/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newProduct)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(false));
        }
    }
}
