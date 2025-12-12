package tqs.blacktie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tqs.blacktie.entity.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByBookingId(Long bookingId);
    List<Review> findByBookingProductId(Long productId);
}
