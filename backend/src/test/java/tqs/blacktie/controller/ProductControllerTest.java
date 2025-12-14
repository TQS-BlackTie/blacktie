package tqs.blacktie.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import tqs.blacktie.entity.Product;
import tqs.blacktie.service.ProductService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Nested
    @DisplayName("Get Products Tests")
    class GetProductsTests {

        @Test
        @DisplayName("Should return all available products")
        void whenGetProducts_thenReturnList() {
            Product product1 = new Product("Smoking", "Classic black", 80.0);
            product1.setId(1L);
            product1.setAvailable(true);
            
            Product product2 = new Product("Tuxedo", "Navy blue", 120.0);
            product2.setId(2L);
            product2.setAvailable(true);

            when(productService.getAvailableProducts(null, null, 1L))
                    .thenReturn(Arrays.asList(product1, product2));

            List<Product> result = productController.getProducts(null, null, 1L);

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals("Smoking", result.get(0).getName());
            assertEquals(80.0, result.get(0).getPrice());
            assertEquals(2L, result.get(1).getId());
            assertEquals("Tuxedo", result.get(1).getName());
        }

        @Test
        @DisplayName("Should return empty list when no products")
        void whenNoProducts_thenReturnEmptyList() {
            when(productService.getAvailableProducts(null, null, 1L))
                    .thenReturn(Collections.emptyList());

            List<Product> result = productController.getProducts(null, null, 1L);

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should filter products by name")
        void whenFilterByName_thenReturnFilteredList() {
            Product product = new Product("Smoking", "Classic black", 80.0);
            product.setId(1L);
            product.setAvailable(true);

            when(productService.getAvailableProducts("smoking", null, 1L))
                    .thenReturn(Collections.singletonList(product));

            List<Product> result = productController.getProducts("smoking", null, 1L);

            assertEquals(1, result.size());
            assertEquals("Smoking", result.get(0).getName());
        }

        @Test
        @DisplayName("Should filter products by max price")
        void whenFilterByMaxPrice_thenReturnFilteredList() {
            Product product = new Product("Smoking", "Classic black", 80.0);
            product.setId(1L);
            product.setAvailable(true);

            when(productService.getAvailableProducts(null, 100.0, 1L))
                    .thenReturn(Collections.singletonList(product));

            List<Product> result = productController.getProducts(null, 100.0, 1L);

            assertEquals(1, result.size());
            assertEquals(80.0, result.get(0).getPrice());
        }

        @Test
        @DisplayName("Should filter products by name and max price")
        void whenFilterByNameAndMaxPrice_thenReturnFilteredList() {
            Product product = new Product("Smoking", "Classic black", 80.0);
            product.setId(1L);
            product.setAvailable(true);

            when(productService.getAvailableProducts("smoking", 100.0, 1L))
                    .thenReturn(Collections.singletonList(product));

            List<Product> result = productController.getProducts("smoking", 100.0, 1L);

            assertEquals(1, result.size());
            assertEquals("Smoking", result.get(0).getName());
            assertEquals(80.0, result.get(0).getPrice());
        }
    }

    @Nested
    @DisplayName("Create Product Tests")
    class CreateProductTests {

        @Test
        @DisplayName("Should create product successfully")
        void whenCreateProduct_thenReturnCreated() {
            Product product = new Product("Smoking", "Classic black", 80.0);
            Product savedProduct = new Product("Smoking", "Classic black", 80.0);
            savedProduct.setId(1L);
            savedProduct.setAvailable(true);

            when(productService.createProduct(any(Product.class), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProduct(product, 1L);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            Product body = (Product) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("Smoking", body.getName());
            assertEquals("Classic black", body.getDescription());
            assertEquals(80.0, body.getPrice());
            assertTrue(body.getAvailable());
        }

        @Test
        @DisplayName("Should create product with availability set to true by default")
        void whenCreateProductWithoutAvailability_thenSetTrue() {
            Product product = new Product("Tuxedo", "Navy blue", 120.0);
            Product savedProduct = new Product("Tuxedo", "Navy blue", 120.0);
            savedProduct.setId(2L);
            savedProduct.setAvailable(true);

            when(productService.createProduct(any(Product.class), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProduct(product, 1L);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertTrue(((Product) response.getBody()).getAvailable());
        }

        @Test
        @DisplayName("Should create product with explicit availability false")
        void whenCreateProductWithAvailabilityFalse_thenKeepFalse() {
            Product product = new Product("Suit", "Gray", 90.0);
            product.setAvailable(false);
            
            Product savedProduct = new Product("Suit", "Gray", 90.0);
            savedProduct.setId(3L);
            savedProduct.setAvailable(false);

            when(productService.createProduct(any(Product.class), org.mockito.ArgumentMatchers.eq(1L))).thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProduct(product, 1L);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertFalse(((Product) response.getBody()).getAvailable());
        }
    }

    @Nested
    @DisplayName("Create Product With Image Tests")
    class CreateProductWithImageTests {

        @Test
        @DisplayName("Should create product with image successfully")
        void whenCreateProductWithImage_thenReturnCreated() {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "suit.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            Product savedProduct = new Product("Smoking", "Classic black", 80.0);
            savedProduct.setId(1L);
            savedProduct.setAvailable(true);
            savedProduct.setImageUrl("/api/products/images/test-uuid.jpg");

            when(productService.createProduct(any(Product.class), anyLong()))
                    .thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProductWithImage(
                    "Smoking",
                    "Classic black",
                    80.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    imageFile,
                    1L
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            Product body = (Product) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("Smoking", body.getName());
            assertNotNull(body.getImageUrl());
            assertTrue(body.getImageUrl().contains("/api/products/images/"));
        }

        @Test
        @DisplayName("Should create product without image when image is null")
        void whenCreateProductWithoutImage_thenReturnCreated() {
            Product savedProduct = new Product("Tuxedo", "Navy blue", 120.0);
            savedProduct.setId(2L);
            savedProduct.setAvailable(true);

            when(productService.createProduct(any(Product.class), anyLong()))
                    .thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProductWithImage(
                    "Tuxedo",
                    "Navy blue",
                    120.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    1L
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            Product body = (Product) response.getBody();
            assertEquals("Tuxedo", body.getName());
            assertNull(body.getImageUrl());
        }

        @Test
        @DisplayName("Should create product without image when image is empty")
        void whenCreateProductWithEmptyImage_thenReturnCreated() {
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "image",
                    "empty.jpg",
                    "image/jpeg",
                    new byte[0]
            );

            Product savedProduct = new Product("Suit", "Gray", 90.0);
            savedProduct.setId(3L);
            savedProduct.setAvailable(true);

            when(productService.createProduct(any(Product.class), anyLong()))
                    .thenReturn(savedProduct);

            ResponseEntity<?> response = productController.createProductWithImage(
                    "Suit",
                    "Gray",
                    90.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    emptyFile,
                    1L
            );

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            Product body = (Product) response.getBody();
            assertEquals("Suit", body.getName());
            assertNull(body.getImageUrl());
        }

        @Test
        @DisplayName("Should return forbidden when non-owner tries to create product")
        void whenNonOwnerCreatesProduct_thenReturnForbidden() {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "suit.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(productService.createProduct(any(Product.class), anyLong()))
                    .thenThrow(new IllegalStateException("Only owners can create products"));

            ResponseEntity<?> response = productController.createProductWithImage(
                    "Smoking",
                    "Classic black",
                    80.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    imageFile,
                    1L
            );

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("Only owners can create products", response.getBody());
        }

        @Test
        @DisplayName("Should return bad request when user not found")
        void whenUserNotFound_thenReturnBadRequest() {
            MockMultipartFile imageFile = new MockMultipartFile(
                    "image",
                    "suit.jpg",
                    "image/jpeg",
                    "test image content".getBytes()
            );

            when(productService.createProduct(any(Product.class), anyLong()))
                    .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = productController.createProductWithImage(
                    "Smoking",
                    "Classic black",
                    80.0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    imageFile,
                    999L
            );

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("User not found", response.getBody());
            assertEquals("User not found", response.getBody());
        }
    }

    @Nested
    @DisplayName("Delete Product Tests")
    class DeleteProductTests {

        @Test
        @DisplayName("Should successfully delete product when owner")
        void whenOwnerDeletesProduct_thenReturnNoContent() {
            // No exception means success
            ResponseEntity<?> response = productController.deleteProduct(1L, 1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when product not found")
        void whenProductNotFound_thenReturnBadRequest() {
            doThrow(new IllegalArgumentException("Product not found with id: 999"))
                    .when(productService).deleteProduct(999L, 1L);

            ResponseEntity<?> response = productController.deleteProduct(999L, 1L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Product not found with id: 999", response.getBody());
        }

        @Test
        @DisplayName("Should return forbidden when non-owner tries to delete")
        void whenNonOwnerDeletesProduct_thenReturnForbidden() {
            doThrow(new IllegalStateException("You can only delete your own products"))
                    .when(productService).deleteProduct(1L, 2L);

            ResponseEntity<?> response = productController.deleteProduct(1L, 2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("You can only delete your own products", response.getBody());
        }

        @Test
        @DisplayName("Should return bad request when user not found")
        void whenUserNotFound_thenReturnBadRequest() {
            doThrow(new IllegalArgumentException("User not found with id: 999"))
                    .when(productService).deleteProduct(1L, 999L);

            ResponseEntity<?> response = productController.deleteProduct(1L, 999L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("User not found with id: 999", response.getBody());
        }
    }
}
