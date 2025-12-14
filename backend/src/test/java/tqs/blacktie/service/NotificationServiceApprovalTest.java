package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.NotificationRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Approval Flow Tests")
class NotificationServiceApprovalTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User owner;
    private User renter;
    private Product product;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setName("Owner User");
        owner.setEmail("owner@test.com");

        renter = new User();
        renter.setId(2L);
        renter.setName("Renter User");
        renter.setEmail("renter@test.com");

        product = new Product();
        product.setId(1L);
        product.setName("Tuxedo");
        product.setOwner(owner);

        booking = new Booking();
        booking.setId(1L);
        booking.setRenter(renter);
        booking.setProduct(product);
        booking.setTotalPrice(150.0);
    }

    @Test
    @DisplayName("Should create booking approved notification")
    void testCreateBookingApprovedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createBookingApprovedNotification(renter, booking);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertEquals(renter, notification.getUser());
        assertEquals(Notification.TYPE_BOOKING_APPROVED, notification.getType());
        assertTrue(notification.getMessage().contains("Tuxedo"));
        assertTrue(notification.getMessage().contains("approved"));
        assertTrue(notification.getMessage().contains("payment"));
        assertEquals(booking, notification.getBooking());
        assertFalse(notification.getIsRead());
    }

    @Test
    @DisplayName("Should create booking rejected notification with reason")
    void testCreateBookingRejectedNotification_WithReason() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        String reason = "Product not available for those dates";

        notificationService.createBookingRejectedNotification(renter, booking, reason);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertEquals(renter, notification.getUser());
        assertEquals(Notification.TYPE_BOOKING_REJECTED, notification.getType());
        assertTrue(notification.getMessage().contains("Tuxedo"));
        assertTrue(notification.getMessage().contains("rejected"));
        assertTrue(notification.getMessage().contains(reason));
        assertEquals(booking, notification.getBooking());
        assertFalse(notification.getIsRead());
    }

    @Test
    @DisplayName("Should create booking rejected notification without reason")
    void testCreateBookingRejectedNotification_WithoutReason() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createBookingRejectedNotification(renter, booking, null);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertEquals(renter, notification.getUser());
        assertEquals(Notification.TYPE_BOOKING_REJECTED, notification.getType());
        assertTrue(notification.getMessage().contains("Tuxedo"));
        assertTrue(notification.getMessage().contains("rejected"));
        assertFalse(notification.getMessage().contains("Reason:"));
        assertEquals(booking, notification.getBooking());
    }

    @Test
    @DisplayName("Should create booking rejected notification with empty reason")
    void testCreateBookingRejectedNotification_EmptyReason() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createBookingRejectedNotification(renter, booking, "");

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertFalse(notification.getMessage().contains("Reason:"));
    }

    @Test
    @DisplayName("Should create payment received notification")
    void testCreatePaymentReceivedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createPaymentReceivedNotification(owner, booking);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertEquals(owner, notification.getUser());
        assertEquals(Notification.TYPE_PAYMENT_RECEIVED, notification.getType());
        assertTrue(notification.getMessage().contains("Tuxedo"));
        assertTrue(notification.getMessage().contains("Payment received"));
        assertTrue(notification.getMessage().contains("Renter User"));
        assertEquals(booking, notification.getBooking());
        assertFalse(notification.getIsRead());
    }

    @Test
    @DisplayName("Should handle null product name gracefully")
    void testCreateNotification_NullProductName() {
        product.setName(null);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createBookingApprovedNotification(renter, booking);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        // Should not throw exception, message should still be created
        assertNotNull(notification.getMessage());
    }

    @Test
    @DisplayName("Should handle null renter name gracefully")
    void testCreateNotification_NullRenterName() {
        renter.setName(null);
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);

        notificationService.createPaymentReceivedNotification(owner, booking);

        verify(notificationRepository, times(1)).save(captor.capture());
        
        Notification notification = captor.getValue();
        assertNotNull(notification);
        assertNotNull(notification.getMessage());
    }

    @Test
    @DisplayName("Should verify all notification types are unique")
    void testNotificationTypes_AreUnique() {
        String[] types = {
            Notification.TYPE_NEW_BOOKING,
            Notification.TYPE_BOOKING_CANCELLED_BY_RENTER,
            Notification.TYPE_BOOKING_CANCELLED_BY_OWNER,
            Notification.TYPE_BOOKING_APPROVED,
            Notification.TYPE_BOOKING_REJECTED,
            Notification.TYPE_PAYMENT_RECEIVED
        };

        assertEquals(6, types.length);
        assertEquals(6, java.util.Arrays.stream(types).distinct().count());
    }
}
