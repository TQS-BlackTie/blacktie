package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.Review;
import tqs.blacktie.entity.User;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ReviewRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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

    private Booking makeBooking(Long bookingId, Long renterId, String status) {
        User renter = new User();
        renter.setId(renterId);
        Product product = new Product();
        product.setId(11L);

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
    void createReview_whenUserNotRenter_throws() {
        Booking booking = makeBooking(2L, 5L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(2L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(99L, 2L, 4, "bad"));
    }

    @Test
    void createReview_whenNotCompleted_throws() {
        Booking booking = makeBooking(3L, 7L, Booking.STATUS_PENDING_APPROVAL);
        when(bookingRepository.findById(3L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(7L, 3L, 4, "not completed"));
    }

    @Test
    void createReview_whenAlreadyExists_throws() {
        Booking booking = makeBooking(4L, 8L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(4L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingId(4L)).thenReturn(Optional.of(new Review()));

        assertThrows(IllegalStateException.class, () -> reviewService.createReview(8L, 4L, 5, "exists"));
    }

    @Test
    void createReview_success_savesAndReturnsResponse() {
        Booking booking = makeBooking(10L, 20L, Booking.STATUS_COMPLETED);
        when(bookingRepository.findById(10L)).thenReturn(Optional.of(booking));
        when(reviewRepository.findByBookingId(10L)).thenReturn(Optional.empty());

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        Review saved = new Review(booking, 5, "great");
        saved.setId(123L);
        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        ReviewResponse resp = reviewService.createReview(20L, 10L, 5, "great");

        assertNotNull(resp);
        assertEquals(123L, resp.getId());
        assertEquals(10L, resp.getBookingId());
        assertEquals(5, resp.getRating());
        assertEquals("great", resp.getComment());

        verify(reviewRepository, times(1)).save(captor.capture());
        Review passed = captor.getValue();
        assertEquals(Integer.valueOf(5), passed.getRating());
        assertEquals("great", passed.getComment());
    }

    @Test
    void getReviewByBookingMapping() {
        User renter = new User(); renter.setId(3L);
        Booking booking = new Booking(); booking.setId(11L); booking.setRenter(renter);
        Product prod = new Product(); prod.setId(22L); booking.setProduct(prod);
        Review r = new Review(booking, 5, "ok"); r.setId(77L);

        when(reviewRepository.findByBookingId(11L)).thenReturn(Optional.of(r));

        ReviewResponse resp = reviewService.getReviewByBooking(11L);
        assertNotNull(resp);
        assertEquals(77L, resp.getId());
        assertEquals(22L, resp.getProductId());
    }

    @Test
    void getReviewsByProductMapping() {
        User renter = new User(); renter.setId(3L);
        Booking booking = new Booking(); booking.setId(21L); booking.setRenter(renter);
        Product prod = new Product(); prod.setId(88L); booking.setProduct(prod);
        Review r = new Review(booking, 3, "meh"); r.setId(101L);

        when(reviewRepository.findByBookingProductId(88L)).thenReturn(List.of(r));

        List<ReviewResponse> list = reviewService.getReviewsByProduct(88L);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals(101L, list.get(0).getId());
    }
}
