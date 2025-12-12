package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Review;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ReviewRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    public ReviewResponse createReview(Long userId, Long bookingId, Integer rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Only renter can create a review
        if (!booking.getRenter().getId().equals(userId)) {
            throw new IllegalStateException("User is not the renter of this booking");
        }

        // Only allow reviews for completed bookings
        if (!Booking.STATUS_COMPLETED.equals(booking.getStatus())) {
            throw new IllegalStateException("Only completed bookings can be reviewed");
        }

        // Only one review per booking
        if (reviewRepository.findByBookingId(bookingId).isPresent()) {
            throw new IllegalStateException("A review for this booking already exists");
        }

        Review review = new Review(booking, rating, comment);
        Review saved = reviewRepository.save(review);

        Long productId = booking.getProduct() != null ? booking.getProduct().getId() : null;
        return new ReviewResponse(saved.getId(), bookingId, productId, saved.getRating(), saved.getComment(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByBooking(Long bookingId) {
        return reviewRepository.findByBookingId(bookingId)
            .map(r -> new ReviewResponse(r.getId(), r.getBooking().getId(), r.getBooking() != null && r.getBooking().getProduct() != null ? r.getBooking().getProduct().getId() : null, r.getRating(), r.getComment(), r.getCreatedAt()))
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        return reviewRepository.findByBookingProductId(productId).stream()
            .map(r -> new ReviewResponse(r.getId(), r.getBooking().getId(), r.getBooking() != null && r.getBooking().getProduct() != null ? r.getBooking().getProduct().getId() : null, r.getRating(), r.getComment(), r.getCreatedAt()))
            .collect(Collectors.toList());
    }
}
