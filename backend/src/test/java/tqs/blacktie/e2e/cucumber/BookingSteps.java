package tqs.blacktie.e2e.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.BookingService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class BookingSteps {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User currentOwner;
    private User currentRenter;
    private User otherRenter;
    private Product currentProduct;
    private Booking currentBooking;
    private BookingResponse lastResponse;
    private Exception lastException;

    @Before("@booking")
    public void setUp() {
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        lastException = null;
        lastResponse = null;
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

    @Given("another renter with email {string}")
    public void anotherRenterWithEmail(String email) {
        otherRenter = userRepository.findByEmail(email).orElseGet(() -> {
            User user = new User("Other Renter", email, "password", "renter");
            return userRepository.save(user);
        });
    }

    @Given("a product {string} owned by {string} priced at {double} per day")
    public void aProductOwnedByPricedAt(String name, String ownerEmail, double price) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        // Check if product already exists
        currentProduct = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(name) && p.getOwner().equals(owner))
                .findFirst()
                .orElseGet(() -> {
                    Product product = new Product(name, "Description for " + name, price);
                    product.setOwner(owner);
                    product.setAvailable(true);
                    return productRepository.save(product);
                });
    }

    @Given("a booking exists for {string} by {string} with status {string}")
    public void aBookingExistsForByWithStatus(String productName, String renterEmail, String status) {
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Renter not found"));

        currentBooking = new Booking(renter, product,
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(8),
                product.getPrice() * 3);
        currentBooking.setStatus(status);
        if ("APPROVED".equals(status)) {
            currentBooking.setDeliveryMethod(Booking.DELIVERY_SHIPPING);
        }
        currentBooking = bookingRepository.save(currentBooking);
    }

    @When("{string} creates a booking for {string} from tomorrow for {int} days")
    public void createsABookingForFromTomorrowForDays(String renterEmail, String productName, int days) {
        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Renter not found"));
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        try {
            BookingRequest request = new BookingRequest();
            request.setProductId(product.getId());
            request.setBookingDate(LocalDateTime.now().plusDays(1));
            request.setReturnDate(LocalDateTime.now().plusDays(1 + days));
            lastResponse = bookingService.createBooking(renter.getId(), request);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} approves the booking with delivery method {string} and location {string}")
    public void approvesTheBookingWithDeliveryMethodAndLocation(String ownerEmail, String method, String location) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        try {
            lastResponse = bookingService.approveBooking(currentBooking.getId(), owner.getId(), method, location);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} rejects the booking with reason {string}")
    public void rejectsTheBookingWithReason(String ownerEmail, String reason) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        try {
            lastResponse = bookingService.rejectBooking(currentBooking.getId(), owner.getId(), reason);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} pays for the booking")
    public void paysForTheBooking(String renterEmail) {
        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Renter not found"));
        try {
            lastResponse = bookingService.processPayment(currentBooking.getId(), renter.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} cancels the booking")
    public void cancelsTheBooking(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            bookingService.cancelBooking(currentBooking.getId(), user.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} tries to cancel the booking")
    public void triesToCancelTheBooking(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            bookingService.cancelBooking(currentBooking.getId(), user.getId());
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @Then("the booking should be created with status {string}")
    public void theBookingShouldBeCreatedWithStatus(String status) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getStatus()).isEqualTo(status);
    }

    @Then("the total price should be {double}")
    public void theTotalPriceShouldBe(double price) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getTotalPrice()).isEqualTo(price);
    }

    @Then("the booking status should be {string}")
    public void theBookingStatusShouldBe(String status) {
        if (lastResponse != null) {
            assertThat(lastResponse.getStatus()).isEqualTo(status);
        } else {
            Booking booking = bookingRepository.findById(currentBooking.getId()).orElseThrow();
            assertThat(booking.getStatus()).isEqualTo(status);
        }
    }

    @Then("the delivery method should be {string}")
    public void theDeliveryMethodShouldBe(String method) {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getDeliveryMethod()).isEqualTo(method);
    }

    @Then("a delivery code should be generated")
    public void aDeliveryCodeShouldBeGenerated() {
        assertThat(lastResponse).isNotNull();
        assertThat(lastResponse.getDeliveryCode()).isNotNull();
    }

    @Then("the renter should be notified of the cancellation")
    public void theRenterShouldBeNotifiedOfTheCancellation() {
        long count = notificationRepository.countByUserAndIsRead(currentRenter, false);
        assertThat(count).isGreaterThan(0);
    }

    @Then("a forbidden error should be returned")
    public void aForbiddenErrorShouldBeReturned() {
        assertThat(lastException).isNotNull();
        assertThat(lastException).isInstanceOf(IllegalStateException.class);
        assertThat(lastException.getMessage()).containsIgnoringCase("not authorized");
    }
}
