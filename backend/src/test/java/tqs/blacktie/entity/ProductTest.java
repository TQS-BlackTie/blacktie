package tqs.blacktie.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Product Entity Tests")
class ProductTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create product with no-args constructor")
        void whenNoArgsConstructor_thenCreateProduct() {
            Product product = new Product();

            assertNotNull(product);
            assertNotNull(product.getCreatedAt());
        }

        @Test
        @DisplayName("Should create product with 3-args constructor")
        void whenThreeArgsConstructor_thenCreateProduct() {
            Product product = new Product("Smoking", "Classic black smoking suit", 80.0);

            assertEquals("Smoking", product.getName());
            assertEquals("Classic black smoking suit", product.getDescription());
            assertEquals(80.0, product.getPrice());
            assertNotNull(product.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void whenSetId_thenGetId() {
            Product product = new Product();
            product.setId(1L);

            assertEquals(1L, product.getId());
        }

        @Test
        @DisplayName("Should get and set name")
        void whenSetName_thenGetName() {
            Product product = new Product();
            product.setName("Tuxedo");

            assertEquals("Tuxedo", product.getName());
        }

        @Test
        @DisplayName("Should get and set description")
        void whenSetDescription_thenGetDescription() {
            Product product = new Product();
            product.setDescription("Navy blue tuxedo");

            assertEquals("Navy blue tuxedo", product.getDescription());
        }

        @Test
        @DisplayName("Should get and set price")
        void whenSetPrice_thenGetPrice() {
            Product product = new Product();
            product.setPrice(150.0);

            assertEquals(150.0, product.getPrice());
        }

        @Test
        @DisplayName("Should get and set available")
        void whenSetAvailable_thenGetAvailable() {
            Product product = new Product();
            product.setAvailable(true);

            assertTrue(product.getAvailable());

            product.setAvailable(false);

            assertFalse(product.getAvailable());
        }

        @Test
        @DisplayName("Should get and set createdAt")
        void whenSetCreatedAt_thenGetCreatedAt() {
            Product product = new Product();
            LocalDateTime now = LocalDateTime.now();
            product.setCreatedAt(now);

            assertEquals(now, product.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Available should be null initially")
        void whenNewProduct_thenAvailableIsNull() {
            Product product = new Product("Smoking", "Black", 80.0);

            assertNull(product.getAvailable());
        }

        @Test
        @DisplayName("CreatedAt should be set on construction")
        void whenNewProduct_thenCreatedAtIsSet() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            Product product = new Product("Smoking", "Black", 80.0);
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertNotNull(product.getCreatedAt());
            assertTrue(product.getCreatedAt().isAfter(before));
            assertTrue(product.getCreatedAt().isBefore(after));
        }

        @Test
        @DisplayName("Id should be null initially")
        void whenNewProduct_thenIdIsNull() {
            Product product = new Product("Smoking", "Black", 80.0);

            assertNull(product.getId());
        }
    }
}
