package tqs.blacktie.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ReviewEntityTest {

    @Test
    void constructorAndAccessors() {
        User u = new User("A","a@x.com","p"); u.setId(2L);
        Product p = new Product(); p.setId(3L);
        Booking b = new Booking(); b.setId(4L); b.setRenter(u); b.setProduct(p);

        Review review = new Review(b, 5, "nice");
        review.setId(77L);
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);

        assertEquals(77L, review.getId());
        assertEquals(b, review.getBooking());
        assertEquals(5, review.getRating());
        assertEquals("nice", review.getComment());
        assertEquals(now, review.getCreatedAt());

        review.setRating(3);
        review.setComment("meh");
        assertEquals(3, review.getRating());
        assertEquals("meh", review.getComment());
    }
}
