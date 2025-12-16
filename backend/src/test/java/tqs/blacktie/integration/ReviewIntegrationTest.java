package tqs.blacktie.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReviewIntegrationTest {

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

    private User owner;
    private User renter;
    private Product product;
    private Booking completedBooking;

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

        product = new Product("Test Suit", "A nice test suit", 100.0);
        product.setOwner(owner);
        product.setAvailable(true);
        product = productRepository.save(product);

        completedBooking = new Booking(renter, product,
                LocalDateTime.now().minusDays(5), LocalDateTime.now().minusDays(3), 100.0);
        completedBooking.setStatus(Booking.STATUS_COMPLETED);
        completedBooking = bookingRepository.save(completedBooking);
    }

    @Test
    void testRenterCreateReviewIntegration() {
        ReviewResponse response = reviewService.createReview(
                renter.getId(), completedBooking.getId(), 5, "Great suit!");

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("Great suit!");
        assertThat(response.getReviewType()).isEqualTo("OWNER"); // Renter reviews Owner
    }

    @Test
    void testOwnerCreateReviewIntegration() {
        ReviewResponse response = reviewService.createReview(
                owner.getId(), completedBooking.getId(), 4, "Good renter");

        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(4);
        assertThat(response.getReviewType()).isEqualTo("RENTER"); // Owner reviews Renter
    }

    @Test
    void testCannotReviewNonCompletedBookingIntegration() {
        Booking pendingBooking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3), 100.0);
        pendingBooking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        pendingBooking = bookingRepository.save(pendingBooking);

        Long bookingId = pendingBooking.getId();

        assertThatThrownBy(() -> reviewService.createReview(renter.getId(), bookingId, 5, "Great!"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only completed bookings can be reviewed");
    }

    @Test
    void testCannotReviewTwiceIntegration() {
        reviewService.createReview(renter.getId(), completedBooking.getId(), 5, "First review");

        Long bookingId = completedBooking.getId();

        assertThatThrownBy(() -> reviewService.createReview(renter.getId(), bookingId, 4, "Second review"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already reviewed");
    }

    @Test
    void testBothPartiesCanReviewSameBookingIntegration() {
        // Renter reviews Owner
        ReviewResponse renterReview = reviewService.createReview(
                renter.getId(), completedBooking.getId(), 5, "Great owner!");

        // Owner reviews Renter
        ReviewResponse ownerReview = reviewService.createReview(
                owner.getId(), completedBooking.getId(), 4, "Nice renter!");

        assertThat(renterReview.getReviewType()).isEqualTo("OWNER");
        assertThat(ownerReview.getReviewType()).isEqualTo("RENTER");
    }

    @Test
    void testGetUserReputationIntegration() {
        // Create a review for the owner
        reviewService.createReview(renter.getId(), completedBooking.getId(), 5, "Excellent!");

        UserResponse reputation = reviewService.getUserReputation(owner.getId());

        assertThat(reputation.getOwnerReviewCount()).isEqualTo(1);
        assertThat(reputation.getOwnerAverageRating()).isEqualTo(5.0);
    }

    @Test
    void testGetReviewByBookingIntegration() {
        reviewService.createReview(renter.getId(), completedBooking.getId(), 4, "Nice suit!");

        ReviewResponse review = reviewService.getReviewByBooking(completedBooking.getId());

        assertThat(review).isNotNull();
        assertThat(review.getRating()).isEqualTo(4);
        assertThat(review.getComment()).isEqualTo("Nice suit!");
    }

    @Test
    void testGetReviewsByProductIntegration() {
        // Create another completed booking
        Booking anotherBooking = new Booking(renter, product,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(8), 100.0);
        anotherBooking.setStatus(Booking.STATUS_COMPLETED);
        anotherBooking = bookingRepository.save(anotherBooking);

        reviewService.createReview(renter.getId(), completedBooking.getId(), 5, "Great!");
        reviewService.createReview(renter.getId(), anotherBooking.getId(), 4, "Good!");

        List<ReviewResponse> reviews = reviewService.getReviewsByProduct(product.getId());

        assertThat(reviews).hasSize(2);
    }
}
