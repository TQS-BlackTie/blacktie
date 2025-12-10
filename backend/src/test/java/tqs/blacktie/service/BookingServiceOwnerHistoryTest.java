package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Owner History Tests")
class BookingServiceOwnerHistoryTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingService bookingService;

    private User owner;
    private User renter;
    private Product product;
    private Booking pastBooking;
    private Booking futureBooking;

    @BeforeEach
    void setUp() {
        owner = new User("Owner", "owner@example.com", "pass", "owner");
        owner.setId(10L);

        renter = new User("Renter", "renter@example.com", "pass");
        renter.setId(20L);

        product = new Product("Tux", "desc", 50.0);
        product.setId(1L);
        product.setOwner(owner);

        LocalDateTime now = LocalDateTime.now();
        pastBooking = new Booking(renter, product, now.minusDays(5), now.minusDays(2), 150.0);
        pastBooking.setId(100L);

        futureBooking = new Booking(renter, product, now.plusDays(2), now.plusDays(4), 100.0);
        futureBooking.setId(101L);
    }

    @Test
    @DisplayName("Should return owner bookings with correct status")
    void shouldReturnOwnerBookingsWithStatus() {
        when(bookingRepository.findByProductOwnerId(10L)).thenReturn(Arrays.asList(pastBooking, futureBooking));

        List<BookingResponse> responses = bookingService.getOwnerBookings(10L);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        BookingResponse r1 = responses.stream().filter(r -> r.getId().equals(100L)).findFirst().orElse(null);
        BookingResponse r2 = responses.stream().filter(r -> r.getId().equals(101L)).findFirst().orElse(null);

        assertNotNull(r1);
        assertNotNull(r2);
        assertEquals(Booking.STATUS_ACTIVE, r1.getStatus());
        assertEquals(Booking.STATUS_ACTIVE, r2.getStatus());
    }
}
