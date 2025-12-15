package tqs.blacktie.e2e.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.ProductService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ProductSteps {

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

    private User currentOwner;
    private User currentRenter;
    private User otherOwner;
    private Product lastCreatedProduct;
    private List<Product> lastProductList;
    private Exception lastException;

    @Before("@product")
    public void setUp() {
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        lastException = null;
        lastCreatedProduct = null;
        lastProductList = null;
    }

    @Given("an owner with email {string}")
    public void anOwnerWithEmail(String email) {
        currentOwner = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("Owner", email, "password", "owner");
            return userRepository.save(user);
        });
    }

    @Given("a renter with email {string}")
    public void aRenterWithEmail(String email) {
        currentRenter = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("Renter", email, "password", "renter");
            return userRepository.save(user);
        });
    }

    @Given("another owner with email {string}")
    public void anotherOwnerWithEmail(String email) {
        otherOwner = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("Other Owner", email, "password", "owner");
            return userRepository.save(user);
        });
    }

    @Given("a product {string} owned by {string} priced at {double}")
    public void aProductOwnedByPricedAt(String name, String ownerEmail, double price) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found: " + ownerEmail));

        Product product = new Product(name, "Description for " + name, price);
        product.setOwner(owner);
        product.setAvailable(true);
        lastCreatedProduct = productRepository.save(product);
    }

    @When("{string} creates a product {string} with description {string} and price {double}")
    public void createsAProductWithDescriptionAndPrice(String email, String name, String desc, double price) {
        User owner = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            Product product = new Product(name, desc, price);
            lastCreatedProduct = productService.createProduct(product, owner.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} tries to create a product {string} with price {double}")
    public void triesToCreateAProductWithPrice(String email, String name, double price) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            Product product = new Product(name, "Description", price);
            lastCreatedProduct = productService.createProduct(product, user.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("I search for products with name {string}")
    public void iSearchForProductsWithName(String name) {
        lastProductList = productService.getAvailableProducts(name, null, currentRenter.getId());
    }

    @When("I search for products with max price {double}")
    public void iSearchForProductsWithMaxPrice(double maxPrice) {
        lastProductList = productService.getAvailableProducts(null, maxPrice, currentRenter.getId());
    }

    @When("{string} deletes the product {string}")
    public void deletesTheProduct(String email, String productName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        try {
            productService.deleteProduct(product.getId(), user.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} tries to delete the product {string}")
    public void triesToDeleteTheProduct(String email, String productName) {
        deletesTheProduct(email, productName);
    }

    @When("{string} views their products")
    public void viewsTheirProducts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        lastProductList = productService.getAvailableProducts(null, null, user.getId());
    }

    @When("{string} browses products")
    public void browsesProducts(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        lastProductList = productService.getAvailableProducts(null, null, user.getId());
    }

    @Then("the product {string} should exist")
    public void theProductShouldExist(String name) {
        boolean exists = productRepository.findAll().stream()
                .anyMatch(p -> p.getName().equals(name));
        assertThat(exists).isTrue();
    }

    @Then("the product should be available for booking")
    public void theProductShouldBeAvailableForBooking() {
        assertThat(lastCreatedProduct).isNotNull();
        assertThat(lastCreatedProduct.getAvailable()).isTrue();
    }

    @Then("the product creation should fail with message {string}")
    public void theProductCreationShouldFailWithMessage(String message) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).contains(message);
    }

    @Then("I should find {int} product(s)")
    public void iShouldFindProducts(int count) {
        assertThat(lastProductList).hasSize(count);
    }

    @Then("they should see {int} product(s)")
    public void theyShouldSeeProducts(int count) {
        assertThat(lastProductList).hasSize(count);
    }

    @Then("the product name should be {string}")
    public void theProductNameShouldBe(String name) {
        assertThat(lastProductList).isNotEmpty();
        assertThat(lastProductList.get(0).getName()).isEqualTo(name);
    }

    @Then("the product should be marked as unavailable")
    public void theProductShouldBeMarkedAsUnavailable() {
        Product product = productRepository.findById(lastCreatedProduct.getId()).orElseThrow();
        assertThat(product.getAvailable()).isFalse();
    }

    @Then("a forbidden error should be returned")
    public void aForbiddenErrorShouldBeReturned() {
        assertThat(lastException).isNotNull();
        assertThat(lastException).isInstanceOf(IllegalStateException.class);
    }
}
