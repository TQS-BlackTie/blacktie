package tqs.blacktie.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.BookingService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    private User owner;
    private User renter;
    private Product product;

    @BeforeEach
    void setUp() {
        // Clean up in order to respect FK constraints
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();

        owner = new User("Owner", "owner@test.com", "password", "owner");
        owner = userRepository.save(owner);

        renter = new User("Renter", "renter@test.com", "password", "renter");
        renter = userRepository.save(renter);

        product = new Product();
        product.setName("Test Suit");
        product.setDescription("A nice suit");
        product.setPrice(100.0);
        product.setAvailable(true);
        product.setOwner(owner);
        product = productRepository.save(product);
    }

    @Test
    void testCreateBookingIntegration() {
        BookingRequest request = new BookingRequest();
        request.setProductId(product.getId());
        request.setBookingDate(LocalDateTime.now().plusDays(1));
        request.setReturnDate(LocalDateTime.now().plusDays(3));

        BookingResponse response = bookingService.createBooking(renter.getId(), request);

        assertThat(response).isNotNull();
        assertThat(response.getProductId()).isEqualTo(product.getId());
        assertThat(response.getStatus()).isEqualTo(Booking.STATUS_PENDING_APPROVAL);
    }

    @Test
    void testGetUserBookingsIntegration() {
        // Create a booking
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        bookingRepository.save(booking);

        List<BookingResponse> bookings = bookingService.getUserBookings(renter.getId());

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getRenterName()).isEqualTo("Renter");
    }

    @Test
    void testGetOwnerBookingsIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        bookingRepository.save(booking);

        List<BookingResponse> bookings = bookingService.getOwnerBookings(owner.getId());

        assertThat(bookings).hasSize(1);
        assertThat(bookings.get(0).getProductName()).isEqualTo("Test Suit");
    }

    @Test
    void testApproveBookingIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        booking = bookingRepository.save(booking);

        BookingResponse response = bookingService.approveBooking(
                booking.getId(), owner.getId(), "PICKUP", "Store Location");

        assertThat(response.getStatus()).isEqualTo(Booking.STATUS_APPROVED);
        assertThat(response.getDeliveryMethod()).isEqualTo("PICKUP");
    }

    @Test
    void testRejectBookingIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        booking = bookingRepository.save(booking);

        BookingResponse response = bookingService.rejectBooking(
                booking.getId(), owner.getId(), "Not available");

        assertThat(response.getStatus()).isEqualTo(Booking.STATUS_REJECTED);
    }

    @Test
    void testCancelBookingByRenterIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        booking = bookingRepository.save(booking);

        // cancelBooking returns void, so just verify it doesn't throw
        bookingService.cancelBooking(booking.getId(), renter.getId());

        // Verify the booking is now cancelled
        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(Booking.STATUS_CANCELLED);
    }

    @Test
    void testCancelBookingByOwnerIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        booking = bookingRepository.save(booking);

        bookingService.cancelBooking(booking.getId(), owner.getId());

        Booking cancelled = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(cancelled.getStatus()).isEqualTo(Booking.STATUS_CANCELLED);
    }

    @Test
    void testUnauthorizedCancelFailsIntegration() {
        User otherUser = new User("Other", "other@test.com", "password", "renter");
        otherUser = userRepository.save(otherUser);

        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
        booking = bookingRepository.save(booking);

        Long bookingId = booking.getId();
        Long otherUserId = otherUser.getId();

        assertThatThrownBy(() -> bookingService.cancelBooking(bookingId, otherUserId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not authorized");
    }

    @Test
    void testProcessPaymentIntegration() {
        Booking booking = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod(Booking.DELIVERY_SHIPPING);
        booking = bookingRepository.save(booking);

        BookingResponse response = bookingService.processPayment(booking.getId(), renter.getId());

        assertThat(response.getStatus()).isEqualTo(Booking.STATUS_PAID);
        assertThat(response.getDeliveryCode()).isNotNull();
    }

    @Test
    void testGetPendingApprovalBookingsIntegration() {
        Booking pending = new Booking(renter, product,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(2), 100.0);
        pending.setStatus(Booking.STATUS_PENDING_APPROVAL);
        bookingRepository.save(pending);

        Booking approved = new Booking(renter, product,
                LocalDateTime.now().plusDays(3), LocalDateTime.now().plusDays(4), 100.0);
        approved.setStatus(Booking.STATUS_APPROVED);
        bookingRepository.save(approved);

        List<BookingResponse> pendingBookings = bookingService.getPendingApprovalBookings(owner.getId());

        assertThat(pendingBookings).hasSize(1);
        assertThat(pendingBookings.get(0).getStatus()).isEqualTo(Booking.STATUS_PENDING_APPROVAL);
    }
}
