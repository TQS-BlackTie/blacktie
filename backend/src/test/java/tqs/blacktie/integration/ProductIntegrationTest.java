package tqs.blacktie.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.ProductService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User owner;
    private User renter;

    @BeforeEach
    void setUp() {
        // Clean up in order
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("Owner", "owner@test.com", "password", "owner");
        owner = userRepository.save(owner);

        renter = new User("Renter", "renter@test.com", "password", "renter");
        renter = userRepository.save(renter);
    }

    @Test
    void testCreateProductIntegration() {
        Product product = new Product("Black Suit", "Elegant black suit", 150.0);

        Product created = productService.createProduct(product, owner.getId());

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Black Suit");
        assertThat(created.getOwner().getId()).isEqualTo(owner.getId());
        assertThat(created.getAvailable()).isTrue();
    }

    @Test
    void testRenterCannotCreateProductIntegration() {
        Product product = new Product("Test Suit", "Test description", 100.0);

        assertThatThrownBy(() -> productService.createProduct(product, renter.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only owners can create products");
    }

    @Test
    void testGetAvailableProductsIntegration() {
        Product product1 = new Product("Suit A", "Description A", 100.0);
        product1.setOwner(owner);
        product1.setAvailable(true);
        productRepository.save(product1);

        Product product2 = new Product("Suit B", "Description B", 200.0);
        product2.setOwner(owner);
        product2.setAvailable(true);
        productRepository.save(product2);

        Product product3 = new Product("Unavailable Suit", "Description C", 300.0);
        product3.setOwner(owner);
        product3.setAvailable(false);
        productRepository.save(product3);

        List<Product> products = productService.getAvailableProducts(null, null, renter.getId());

        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Suit A", "Suit B");
    }

    @Test
    void testGetAvailableProductsByNameIntegration() {
        Product product1 = new Product("Black Suit", "Description A", 100.0);
        product1.setOwner(owner);
        product1.setAvailable(true);
        productRepository.save(product1);

        Product product2 = new Product("White Tuxedo", "Description B", 200.0);
        product2.setOwner(owner);
        product2.setAvailable(true);
        productRepository.save(product2);

        List<Product> products = productService.getAvailableProducts("Black", null, renter.getId());

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Black Suit");
    }

    @Test
    void testGetAvailableProductsByMaxPriceIntegration() {
        Product product1 = new Product("Cheap Suit", "Description A", 50.0);
        product1.setOwner(owner);
        product1.setAvailable(true);
        productRepository.save(product1);

        Product product2 = new Product("Expensive Suit", "Description B", 500.0);
        product2.setOwner(owner);
        product2.setAvailable(true);
        productRepository.save(product2);

        List<Product> products = productService.getAvailableProducts(null, 100.0, renter.getId());

        assertThat(products).hasSize(1);
        assertThat(products.get(0).getName()).isEqualTo("Cheap Suit");
    }

    @Test
    void testDeleteProductIntegration() {
        Product product = new Product("Delete Me", "Description", 100.0);
        product.setOwner(owner);
        product.setAvailable(true);
        product = productRepository.save(product);

        productService.deleteProduct(product.getId(), owner.getId());

        // Product should be marked as unavailable, not deleted
        Product deleted = productRepository.findById(product.getId()).orElse(null);
        assertThat(deleted).isNotNull();
        assertThat(deleted.getAvailable()).isFalse();
    }

    @Test
    void testNonOwnerCannotDeleteProductIntegration() {
        Product product = new Product("Not Yours", "Description", 100.0);
        product.setOwner(owner);
        product.setAvailable(true);
        product = productRepository.save(product);

        Long productId = product.getId();

        assertThatThrownBy(() -> productService.deleteProduct(productId, renter.getId()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("You can only delete your own products");
    }

    @Test
    void testOwnerSeesOnlyTheirProductsIntegration() {
        User anotherOwner = new User("Other Owner", "other.owner@test.com", "password", "owner");
        anotherOwner = userRepository.save(anotherOwner);

        Product myProduct = new Product("My Suit", "Description A", 100.0);
        myProduct.setOwner(owner);
        myProduct.setAvailable(true);
        productRepository.save(myProduct);

        Product theirProduct = new Product("Their Suit", "Description B", 200.0);
        theirProduct.setOwner(anotherOwner);
        theirProduct.setAvailable(true);
        productRepository.save(theirProduct);

        // Owner should only see their own products
        List<Product> ownerProducts = productService.getAvailableProducts(null, null, owner.getId());
        assertThat(ownerProducts).hasSize(1);
        assertThat(ownerProducts.get(0).getName()).isEqualTo("My Suit");

        // Renter should see all available products
        List<Product> renterProducts = productService.getAvailableProducts(null, null, renter.getId());
        assertThat(renterProducts).hasSize(2);
    }
}
