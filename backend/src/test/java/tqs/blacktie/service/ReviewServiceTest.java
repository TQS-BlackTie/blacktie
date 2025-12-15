package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.dto.UserResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.Review;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ReviewRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User renter;
    private User owner;
    private User otherUser;
    private Product product;
    private Booking booking;

    @BeforeEach
    void setup() {
        renter = new User();
        renter.setId(20L);
        renter.setName("Renter");

        owner = new User();
        owner.setId(30L);
        owner.setName("Owner");

        otherUser = new User();
        otherUser.setId(99L);

        product = new Product();
        product.setId(11L);
        product.setOwner(owner);

        booking = new Booking();
        booking.setId(10L);
        booking.setRenter(renter);
        booking.setProduct(product);
        booking.setBookingDate(LocalDateTime.now().minusDays(10));
        booking.setReturnDate(LocalDateTime.now().minusDays(7));
        booking.setTotalPrice(30.0);
        booking.setStatus(Booking.STATUS_COMPLETED);
    }

    @Nested
    @DisplayName("Create Review Tests")
    class CreateReviewTests {

        @Test
        @DisplayName("Should throw exception when booking not found")
        void whenBookingNotFound_thenThrowException() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(renter.getId(), 1L, 5, "ok"));
        }

        @Test
        @DisplayName("Should throw exception when booking is not completed")
        void whenBookingNotCompleted_thenThrowException() {
            booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(IllegalStateException.class,
                    () -> reviewService.createReview(renter.getId(), booking.getId(), 5, "not completed"));
        }

        @Test
        @DisplayName("Should throw exception when user is not involved in booking")
        void whenUserNotInvolved_thenThrowException() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(IllegalStateException.class,
                    () -> reviewService.createReview(otherUser.getId(), booking.getId(), 5, "intruder"));
        }

        @Test
        @DisplayName("Should throw exception when review already exists")
        void whenReviewAlreadyExists_thenThrowException() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.findByBookingIdAndReviewType(booking.getId(), "OWNER"))
                    .thenReturn(Optional.of(new Review()));

            assertThrows(IllegalStateException.class,
                    () -> reviewService.createReview(renter.getId(), booking.getId(), 5, "duplicate"));
        }

        @Test
        @DisplayName("Renter reviewing Owner (Type: OWNER) - Success")
        void whenRenterReviewsOwner_thenSuccess() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.findByBookingIdAndReviewType(booking.getId(), "OWNER"))
                    .thenReturn(Optional.empty());

            Review savedReview = new Review(booking, 5, "Great product!", "OWNER");
            savedReview.setId(100L);
            savedReview.setCreatedAt(LocalDateTime.now());

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            ReviewResponse response = reviewService.createReview(renter.getId(), booking.getId(), 5, "Great product!");

            assertNotNull(response);
            assertEquals(5, response.getRating());
            assertEquals("OWNER", response.getReviewType());

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            Review captured = captor.getValue();
            assertEquals("OWNER", captured.getReviewType());
            assertEquals(5, captured.getRating());
        }

        @Test
        @DisplayName("Owner reviewing Renter (Type: RENTER) - Success")
        void whenOwnerReviewsRenter_thenSuccess() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.findByBookingIdAndReviewType(booking.getId(), "RENTER"))
                    .thenReturn(Optional.empty());

            Review savedReview = new Review(booking, 4, "Good renter", "RENTER");
            savedReview.setId(101L);
            savedReview.setCreatedAt(LocalDateTime.now());

            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            ReviewResponse response = reviewService.createReview(owner.getId(), booking.getId(), 4, "Good renter");

            assertNotNull(response);
            assertEquals(4, response.getRating());
            assertEquals("RENTER", response.getReviewType());

            ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
            verify(reviewRepository).save(captor.capture());
            Review captured = captor.getValue();
            assertEquals("RENTER", captured.getReviewType());
            assertEquals(4, captured.getRating());
        }
    }

    @Nested
    @DisplayName("User Reputation Tests")
    class UserReputationTests {

        @Test
        @DisplayName("Should calculate reputation correctly with mixed reviews")
        void whenUserHasReviews_thenCalculateCorrectly() {
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));

            // Reviews received as OWNER (People reviewed me as an owner)
            Review r1 = new Review();
            r1.setRating(5);
            Review r2 = new Review();
            r2.setRating(3);
            when(reviewRepository.findByBookingProductOwnerIdAndReviewType(owner.getId(), "OWNER"))
                    .thenReturn(Arrays.asList(r1, r2));

            // Reviews received as RENTER (People reviewed me as a renter)
            Review r3 = new Review();
            r3.setRating(4);
            when(reviewRepository.findByBookingRenterIdAndReviewType(owner.getId(), "RENTER"))
                    .thenReturn(Collections.singletonList(r3));

            UserResponse response = reviewService.getUserReputation(owner.getId());

            assertNotNull(response);
            assertEquals(owner.getId(), response.getId());

            // Owner Stats: Avg (5+3)/2 = 4.0, Count 2
            assertEquals(4.0, response.getOwnerAverageRating());
            assertEquals(2, response.getOwnerReviewCount());

            // Renter Stats: Avg 4.0, Count 1
            assertEquals(4.0, response.getRenterAverageRating());
            assertEquals(1, response.getRenterReviewCount());

            // Total: (5+3+4)/3 = 4.0, Count 3
            assertEquals(4.0, response.getAverageRating());
            assertEquals(3, response.getTotalReviews());
        }

        @Test
        @DisplayName("Should return zeros when user has no reviews")
        void whenUserHasNoReviews_thenReturnsZeros() {
            when(userRepository.findById(owner.getId())).thenReturn(Optional.of(owner));
            when(reviewRepository.findByBookingProductOwnerIdAndReviewType(owner.getId(), "OWNER"))
                    .thenReturn(Collections.emptyList());
            when(reviewRepository.findByBookingRenterIdAndReviewType(owner.getId(), "RENTER"))
                    .thenReturn(Collections.emptyList());

            UserResponse response = reviewService.getUserReputation(owner.getId());

            assertNotNull(response);
            assertEquals(0.0, response.getOwnerAverageRating());
            assertEquals(0, response.getOwnerReviewCount());
            assertEquals(0.0, response.getRenterAverageRating());
            assertEquals(0, response.getRenterReviewCount());
            assertEquals(0.0, response.getAverageRating());
            assertEquals(0, response.getTotalReviews());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void whenUserNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> reviewService.getUserReputation(999L));
        }
    }

    @Nested
    @DisplayName("Get Reviews Tests")
    class GetReviewsTests {

        @Test
        @DisplayName("Should return review by booking ID (Legacy/Default behavior)")
        void getReviewByBooking_returnsOwnerTypeReview() {
            Review r = new Review(booking, 5, "ok", "OWNER");
            r.setId(77L);
            r.setCreatedAt(LocalDateTime.now());

            when(reviewRepository.findByBookingIdAndReviewType(booking.getId(), "OWNER"))
                    .thenReturn(Optional.of(r));

            ReviewResponse response = reviewService.getReviewByBooking(booking.getId());

            assertNotNull(response);
            assertEquals(77L, response.getId());
            assertEquals("OWNER", response.getReviewType());
        }

        @Test
        @DisplayName("Should return ALL reviews for a product (both OWNER and RENTER types)")
        void getReviewsByProduct_returnsAllTypes() {
            // Review 1: Renter reviews Owner (Type: OWNER)
            Review r1 = new Review(booking, 5, "Great product", "OWNER");
            r1.setId(1L);
            r1.setCreatedAt(LocalDateTime.now());

            // Review 2: Owner reviews Renter (Type: RENTER)
            Review r2 = new Review(booking, 4, "Good renter", "RENTER");
            r2.setId(2L);
            r2.setCreatedAt(LocalDateTime.now());

            when(reviewRepository.findByBookingProductId(product.getId()))
                    .thenReturn(Arrays.asList(r1, r2));

            List<ReviewResponse> responses = reviewService.getReviewsByProduct(product.getId());

            assertNotNull(responses);
            assertEquals(2, responses.size());

            // Verify both types are present
            assertTrue(responses.stream().anyMatch(r -> "OWNER".equals(r.getReviewType())));
            assertTrue(responses.stream().anyMatch(r -> "RENTER".equals(r.getReviewType())));
        }

        @Test
        @DisplayName("Should handle empty comment text")
        void whenEmptyComment_thenSuccess() {
            when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(reviewRepository.findByBookingIdAndReviewType(booking.getId(), "OWNER"))
                    .thenReturn(Optional.empty());

            Review savedReview = new Review(booking, 4, "", "OWNER");
            savedReview.setId(200L);
            savedReview.setCreatedAt(LocalDateTime.now());
            when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);

            ReviewResponse response = reviewService.createReview(renter.getId(), booking.getId(), 4, "");

            assertNotNull(response);
            assertEquals("", response.getComment());
        }
    }
}
