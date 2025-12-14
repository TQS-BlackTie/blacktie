package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.dto.ReviewResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Review;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ReviewRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public ReviewService(ReviewRepository reviewRepository, BookingRepository bookingRepository,
            UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ReviewResponse createReview(Long userId, Long bookingId, Integer rating, String comment) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Only allow reviews for completed bookings
        if (!Booking.STATUS_COMPLETED.equals(booking.getStatus())) {
            throw new IllegalStateException("Only completed bookings can be reviewed");
        }

        String reviewType;
        if (booking.getRenter().getId().equals(userId)) {
            // Renter is reviewing the Owner/Product
            reviewType = "OWNER";
        } else if (booking.getProduct().getOwner().getId().equals(userId)) {
            // Owner is reviewing the Renter
            reviewType = "RENTER";
        } else {
            throw new IllegalStateException("User is not the renter or owner of this booking");
        }

        // Only one review of THIS type per booking
        if (reviewRepository.findByBookingIdAndReviewType(bookingId, reviewType).isPresent()) {
            throw new IllegalStateException("You have already reviewed this booking");
        }

        Review review = new Review(booking, rating, comment, reviewType);
        Review saved = reviewRepository.save(review);

        Long productId = booking.getProduct() != null ? booking.getProduct().getId() : null;
        ReviewResponse response = new ReviewResponse();
        response.setId(saved.getId());
        response.setBookingId(bookingId);
        response.setProductId(productId);
        response.setRating(saved.getRating());
        response.setComment(saved.getComment());
        response.setCreatedAt(saved.getCreatedAt());
        response.setReviewType(saved.getReviewType());
        return response;
    }

    @Transactional(readOnly = true)
    public tqs.blacktie.dto.UserResponse getUserReputation(Long userId) {
        tqs.blacktie.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get reviews AS OWNER (Type = OWNER)
        List<Review> ownerReviews = reviewRepository.findByBookingProductOwnerIdAndReviewType(userId, "OWNER");

        // Get reviews AS RENTER (Type = RENTER)
        List<Review> renterReviews = reviewRepository.findByBookingRenterIdAndReviewType(userId, "RENTER");

        tqs.blacktie.dto.UserResponse response = new tqs.blacktie.dto.UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setBusinessInfo(user.getBusinessInfo());

        // Calculate stats manually since we removed helper class
        // Owner Stats
        if (ownerReviews.isEmpty()) {
            response.setOwnerAverageRating(0.0);
            response.setOwnerReviewCount(0);
        } else {
            response.setOwnerAverageRating(ownerReviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average().orElse(0.0));
            response.setOwnerReviewCount(ownerReviews.size());
        }

        // Renter Stats
        if (renterReviews.isEmpty()) {
            response.setRenterAverageRating(0.0);
            response.setRenterReviewCount(0);
        } else {
            response.setRenterAverageRating(renterReviews.stream()
                    .filter(r -> r.getRating() != null)
                    .mapToInt(Review::getRating)
                    .average().orElse(0.0));
            response.setRenterReviewCount(renterReviews.size());
        }

        // Total
        int totalCount = ownerReviews.size() + renterReviews.size();
        double totalSum = (double) ownerReviews.stream()
                .filter(r -> r.getRating() != null)
                .mapToInt(Review::getRating).sum() +
                (double) renterReviews.stream()
                        .filter(r -> r.getRating() != null)
                        .mapToInt(Review::getRating).sum();

        response.setTotalReviews(totalCount);
        response.setAverageRating(totalCount > 0 ? totalSum / totalCount : 0.0);

        return response;
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReviewByBooking(Long bookingId) {
        // Return Renter's review of Owner as default for now, or change API to return
        // list
        // Keeping backward compatibility for product page showing "Review" (usually by
        // Renter)
        // Adjusting to find 'OWNER' type review (Review OF Owner BY Renter)
        return reviewRepository.findByBookingIdAndReviewType(bookingId, "OWNER")
                .map(r -> {
                    ReviewResponse resp = new ReviewResponse();
                    resp.setId(r.getId());
                    resp.setBookingId(r.getBooking().getId());
                    resp.setProductId(r.getBooking() != null && r.getBooking().getProduct() != null
                            ? r.getBooking().getProduct().getId()
                            : null);
                    resp.setRating(r.getRating());
                    resp.setComment(r.getComment());
                    resp.setCreatedAt(r.getCreatedAt());
                    resp.setReviewType(r.getReviewType());
                    return resp;
                })
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getReviewsByProduct(Long productId) {
        // Get ALL reviews for the product (both OWNER and RENTER types)
        // The frontend will need to see both types for the product bookings modal
        return reviewRepository.findByBookingProductId(productId).stream()
                .map(r -> {
                    ReviewResponse resp = new ReviewResponse();
                    resp.setId(r.getId());
                    resp.setBookingId(r.getBooking().getId());
                    resp.setProductId(r.getBooking() != null && r.getBooking().getProduct() != null
                            ? r.getBooking().getProduct().getId()
                            : null);
                    resp.setRating(r.getRating());
                    resp.setComment(r.getComment());
                    resp.setCreatedAt(r.getCreatedAt());
                    resp.setReviewType(r.getReviewType());
                    return resp;
                })
                .collect(Collectors.toList());
    }
}
