package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceApprovalTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private BookingService bookingService;

    private User owner;
    private User renter;
    private Product product;
    private Booking booking;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setRole("owner");

        renter = new User();
        renter.setId(2L);

        product = new Product();
        product.setId(1L);
        product.setOwner(owner);

        booking = new Booking();
        booking.setId(1L);
        booking.setRenter(renter);
        booking.setProduct(product);
        booking.setStatus(Booking.STATUS_PENDING_APPROVAL);
    }

    @Test
    void testGetPendingApprovalBookings() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(bookingRepository.findByProductOwnerId(1L)).thenReturn(Arrays.asList(booking));

        var result = bookingService.getPendingApprovalBookings(1L);

        assertEquals(1, result.size());
        verify(bookingRepository).findByProductOwnerId(1L);
    }

    @Test
    void testGetPendingApprovalBookings_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
            () -> bookingService.getPendingApprovalBookings(1L));
    }

    @Test
    void testGetPendingApprovalBookings_NotOwner() {
        owner.setRole("renter");
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThrows(IllegalStateException.class, 
            () -> bookingService.getPendingApprovalBookings(1L));
    }

    @Test
    void testApproveBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BookingResponse result = bookingService.approveBooking(1L, 1L, "PICKUP", "123 Main St");

        assertEquals(Booking.STATUS_APPROVED, result.getStatus());
        assertEquals("PICKUP", result.getDeliveryMethod());
        verify(notificationService).createBookingApprovedNotification(any(), any());
    }

    @Test
    void testApproveBooking_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
            () -> bookingService.approveBooking(1L, 1L, "PICKUP", "123 Main St"));
    }

    @Test
    void testApproveBooking_NotOwner() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, 
            () -> bookingService.approveBooking(1L, 999L, "PICKUP", "123 Main St"));
    }

    @Test
    void testApproveBooking_NotPending() {
        booking.setStatus(Booking.STATUS_APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, 
            () -> bookingService.approveBooking(1L, 1L, "PICKUP", "123 Main St"));
    }

    @Test
    void testApproveBooking_InvalidDeliveryMethod() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, 
            () -> bookingService.approveBooking(1L, 1L, "INVALID", null));
    }

    @Test
    void testApproveBooking_MissingPickupLocation() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalArgumentException.class, 
            () -> bookingService.approveBooking(1L, 1L, "PICKUP", null));
    }

    @Test
    void testRejectBooking_Success() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BookingResponse result = bookingService.rejectBooking(1L, 1L, "Not available");

        assertEquals(Booking.STATUS_REJECTED, result.getStatus());
        verify(notificationService).createBookingRejectedNotification(any(), any(), eq("Not available"));
    }

    @Test
    void testRejectBooking_NotFound() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, 
            () -> bookingService.rejectBooking(1L, 1L, "reason"));
    }

    @Test
    void testProcessPayment_Success() {
        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod("SHIPPING");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any())).thenAnswer(i -> i.getArguments()[0]);

        BookingResponse result = bookingService.processPayment(1L, 2L);

        assertEquals(Booking.STATUS_PAID, result.getStatus());
        assertNotNull(result.getDeliveryCode());
        verify(notificationService).createPaymentReceivedNotification(any(), any());
    }

    @Test
    void testProcessPayment_NotRenter() {
        booking.setStatus(Booking.STATUS_APPROVED);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, 
            () -> bookingService.processPayment(1L, 999L));
    }

    @Test
    void testProcessPayment_NotApproved() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));

        assertThrows(IllegalStateException.class, 
            () -> bookingService.processPayment(1L, 2L));
    }
}
