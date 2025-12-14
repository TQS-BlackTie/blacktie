package tqs.blacktie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tqs.blacktie.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
    // Find reviews where the Owner is the subject (written by Renter)
    List<Review> findByBookingProductOwnerIdAndReviewType(Long ownerId, String reviewType);
    
    // Find reviews where the Renter is the subject (written by Owner)
    List<Review> findByBookingRenterIdAndReviewType(Long renterId, String reviewType);

    List<Review> findByBookingProductId(Long productId);
    
    // Check if a specific review type exists for a booking
    Optional<Review> findByBookingIdAndReviewType(Long bookingId, String reviewType);
}
