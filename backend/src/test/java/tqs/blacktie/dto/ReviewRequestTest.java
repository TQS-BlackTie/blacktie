package tqs.blacktie.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewRequestTest {

    @Test
    void gettersAndSetters() {
        ReviewRequest req = new ReviewRequest(10L, 4, "good");
        assertEquals(10L, req.getBookingId());
        assertEquals(4, req.getRating());
        assertEquals("good", req.getComment());

        req.setBookingId(20L);
        req.setRating(5);
        req.setComment("better");

        assertEquals(20L, req.getBookingId());
        assertEquals(5, req.getRating());
        assertEquals("better", req.getComment());
    }
}
