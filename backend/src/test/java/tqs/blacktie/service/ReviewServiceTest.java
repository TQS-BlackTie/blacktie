package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.Review;
import tqs.blacktie.entity.User;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.dto.UserResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class ReviewServiceTest {

    private ReviewRepository reviewRepository;
    private BookingRepository bookingRepository;
    private UserRepository userRepository;
    private ReviewService reviewService;

    @BeforeEach
    void setup() {
        reviewRepository = mock(ReviewRepository.class);
        bookingRepository = mock(BookingRepository.class);
        userRepository = mock(UserRepository.class);

        reviewService = new ReviewService(reviewRepository, bookingRepository, userRepository);
    }

    private Booking makeBooking(Long bookingId, Long renterId, Long ownerId, String status) {
        User renter = new User();
        renter.setId(renterId);
        
        User owner = new User();
        owner.setId(ownerId);
        
        Product product = new Product();
        product.setId(11L);
        product.setOwner(owner);

        Booking booking = new Booking();
        booking.setId(bookingId);
        booking.setRenter(renter);
        booking.setProduct(product);
        booking.setBookingDate(LocalDateTime.now().minusDays(10));
        booking.setReturnDate(LocalDateTime.now().minusDays(7));
        booking.setTotalPrice(30.0);
        booking.setStatus(status);
        return booking;
    }

    @Test
    void createReview_whenBookingNotFound_throws() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> reviewService.createReview(2L, 1L, 5, "ok"));
    }

    @Test
    void createReview_whenUserNotRenterOrOwner_throws() {
        Booking booking = makeBooking(2L, 5L, 6L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(99L, 2L, 4, "bad"));
    }

    @Test
    void createReview_whenNotCompleted_throws() {
        Booking booking = makeBooking(3L, 7L, 8L, Booking.STATUS_PENDING_APPROVAL);
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(7L, 3L, 4, "not completed"));
    }

    @Test
    void createReview_whenAlreadyExists_throws() {
        Booking booking = makeBooking(4L, 8L, 9L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));
        // Mock that a review of type OWNER already exists (Renter reviewing Owner)
        when(reviewRepository.findByBookingIdAndReviewType(4L, "OWNER")).thenReturn(Optional.of(new Review()));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(8L, 4L, 5, "exists"));
    }

    @Test
    void createReview_RenterReviewsOwner_success() {
        Booking booking = makeBooking(10L, 20L, 30L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingIdAndReviewType(10L, "OWNER")).thenReturn(Optional.empty());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        Review saved = new Review(booking, 5, "great", "OWNER");
        saved.setId(123L);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = reviewService.createReview(20L, 10L, 5, "great");

        assertNotNull(resp);
        assertEquals(5, resp.getRating());

        verify(reviewRepository, times(1)).save(captor.capture());
        Review passed = captor.getValue();
        assertEquals("OWNER", passed.getReviewType());
        assertEquals(5, passed.getRating());
    }
    
    @Test
    void createReview_OwnerReviewsRenter_success() {
        Booking booking = makeBooking(11L, 20L, 30L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(11L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingIdAndReviewType(11L, "RENTER")).thenReturn(Optional.empty());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        Review saved = new Review(booking, 4, "good renter", "RENTER");
        saved.setId(124L);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = reviewService.createReview(30L, 11L, 4, "good renter");

        assertNotNull(resp);
        
        verify(reviewRepository, times(1)).save(captor.capture());
        Review passed = captor.getValue();
        assertEquals("RENTER", passed.getReviewType());
        assertEquals(4, passed.getRating());
    }

    @Test
    void getUserReputation_calculatesCorrectly() {
        Long userId = 100L;
        User user = new User(); user.setId(userId); user.setName("Test User");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // Reviews received as OWNER (People reviewed me as an owner)
        Review r1 = new Review(); r1.setRating(5);
        Review r2 = new Review(); r2.setRating(3);
        when(reviewRepository.findByBookingProductOwnerIdAndReviewType(userId, "OWNER"))
            .thenReturn(Arrays.asList(r1, r2));

        // Reviews received as RENTER (People reviewed me as a renter)
        Review r3 = new Review(); r3.setRating(4);
        when(reviewRepository.findByBookingRenterIdAndReviewType(userId, "RENTER"))
            .thenReturn(Collections.singletonList(r3));

        tqs.blacktie.dto.UserResponse response = reviewService.getUserReputation(userId);

        assertNotNull(response);
        assertEquals(userId, response.getId());
        
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
    void getReviewByBookingMapping_returnsOwnerType() {
        // Legacy behavior check
        User renter = new User(); renter.setId(3L);
        Booking booking = new Booking(); booking.setId(11L); booking.setRenter(renter);
        Product prod = new Product(); prod.setId(22L); booking.setProduct(prod);
        
        Review r = new Review(booking, 5, "ok", "OWNER"); r.setId(77L);

        when(reviewRepository.findByBookingIdAndReviewType(11L, "OWNER")).thenReturn(Optional.of(r));

        ReviewResponse resp = reviewService.getReviewByBooking(11L);
        assertNotNull(resp);
        assertEquals(77L, resp.getId());
    }
}
