package tqs.blacktie.e2e.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.dto.UserResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.ReviewService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ReviewSteps {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    private User currentOwner;
    private User currentRenter;
    private Product currentProduct;
    private Booking currentBooking;
    private Booking pendingBooking;
    private ReviewResponse lastReview;
    private List<ReviewResponse> lastReviewList;
    private Exception lastException;

    @Before("@review")
    public void setUp() {
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        lastException = null;
        lastReview = null;
        lastReviewList = null;
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

    @Given("a product {string} owned by {string} priced at {double}")
    public void aProductOwnedByPricedAt(String name, String ownerEmail, double price) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        currentProduct = new Product(name, "Description for " + name, price);
        currentProduct.setOwner(owner);
        currentProduct.setAvailable(true);
        currentProduct = productRepository.save(currentProduct);
    }

    @Given("a completed booking for {string} by {string}")
    public void aCompletedBookingForBy(String productName, String renterEmail) {
        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Renter not found"));
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        currentBooking = new Booking(renter, product,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3),
                product.getPrice() * 2);
        currentBooking.setStatus(Booking.STATUS_COMPLETED);
        currentBooking = bookingRepository.save(currentBooking);
    }

    @Given("a booking exists for {string} by {string} with status {string}")
    public void aBookingExistsForByWithStatus(String productName, String renterEmail, String status) {
        User renter = userRepository.findByEmail(renterEmail)
                .orElseThrow(() -> new IllegalArgumentException("Renter not found"));
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        pendingBooking = new Booking(renter, product,
                LocalDateTime.now().plusDays(5), LocalDateTime.now().plusDays(8),
                product.getPrice() * 3);
        pendingBooking.setStatus(status);
        pendingBooking = bookingRepository.save(pendingBooking);
    }

    @Given("{string} has already reviewed the booking")
    public void hasAlreadyReviewedTheBooking(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        reviewService.createReview(user.getId(), currentBooking.getId(), 5, "Previous review");
    }

    @Given("{string} has reviewed the booking with rating {int}")
    public void hasReviewedTheBookingWithRating(String email, int rating) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        reviewService.createReview(user.getId(), currentBooking.getId(), rating, "Review comment");
    }

    @When("{string} reviews the booking with rating {int} and comment {string}")
    public void reviewsTheBookingWithRatingAndComment(String email, int rating, String comment) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            lastReview = reviewService.createReview(user.getId(), currentBooking.getId(), rating, comment);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} tries to review the booking again")
    public void triesToReviewTheBookingAgain(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            lastReview = reviewService.createReview(user.getId(), currentBooking.getId(), 4, "Second attempt");
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("{string} tries to review the pending booking")
    public void triesToReviewThePendingBooking(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            lastReview = reviewService.createReview(user.getId(), pendingBooking.getId(), 5, "Review");
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("I get reviews for product {string}")
    public void iGetReviewsForProduct(String productName) {
        Product product = productRepository.findAll().stream()
                .filter(p -> p.getName().equals(productName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        lastReviewList = reviewService.getReviewsByProduct(product.getId());
    }

    @Then("the review should be created with type {string}")
    public void theReviewShouldBeCreatedWithType(String type) {
        assertThat(lastReview).isNotNull();
        assertThat(lastReview.getReviewType()).isEqualTo(type);
    }

    @Then("the owner's average rating should be {double}")
    public void theOwnersAverageRatingShouldBe(double rating) {
        UserResponse reputation = reviewService.getUserReputation(currentOwner.getId());
        assertThat(reputation.getOwnerAverageRating()).isEqualTo(rating);
    }

    @Then("the renter's average rating should be {double}")
    public void theRentersAverageRatingShouldBe(double rating) {
        UserResponse reputation = reviewService.getUserReputation(currentRenter.getId());
        assertThat(reputation.getRenterAverageRating()).isEqualTo(rating);
    }

    @Then("the booking should have {int} reviews")
    public void theBookingShouldHaveReviews(int count) {
        long reviewCount = reviewRepository.findByBookingProductId(currentProduct.getId()).size();
        assertThat(reviewCount).isEqualTo(count);
    }

    @Then("the review should fail with message {string}")
    public void theReviewShouldFailWithMessage(String message) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).containsIgnoringCase(message);
    }

    @Then("I should see {int} review(s)")
    public void iShouldSeeReviews(int count) {
        assertThat(lastReviewList).hasSize(count);
    }

    @Then("the review rating should be {int}")
    public void theReviewRatingShouldBe(int rating) {
        assertThat(lastReviewList).isNotEmpty();
        assertThat(lastReviewList.get(0).getRating()).isEqualTo(rating);
    }
}
