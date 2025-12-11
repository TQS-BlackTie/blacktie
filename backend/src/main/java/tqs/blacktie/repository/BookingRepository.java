package tqs.blacktie.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.User;
import tqs.blacktie.entity.Product;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Find all bookings for a specific user
    List<Booking> findByRenter(User renter);

    // Find all bookings for a specific product
    List<Booking> findByProduct(Product product);

    // Find bookings by user ID
    List<Booking> findByRenterId(Long renterId);

    // Find bookings by product ID
    List<Booking> findByProductId(Long productId);

    // Find active bookings for a product (overlapping dates)
    List<Booking> findByProductAndBookingDateLessThanEqualAndReturnDateGreaterThanEqual(
        Product product, LocalDateTime returnDate, LocalDateTime bookingDate);

    // Find bookings for products owned by a specific owner
    @Query("SELECT b FROM Booking b WHERE b.product.owner.id = :ownerId")
    List<Booking> findByProductOwnerId(@Param("ownerId") Long ownerId);
}
