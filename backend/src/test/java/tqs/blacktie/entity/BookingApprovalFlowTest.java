package tqs.blacktie.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking Entity Approval Flow Tests")
class BookingApprovalFlowTest {

    @Test
    @DisplayName("Should have correct status constants")
    void testStatusConstants() {
        assertEquals("PENDING_APPROVAL", Booking.STATUS_PENDING_APPROVAL);
        assertEquals("APPROVED", Booking.STATUS_APPROVED);
        assertEquals("REJECTED", Booking.STATUS_REJECTED);
        assertEquals("PAID", Booking.STATUS_PAID);
        assertEquals("COMPLETED", Booking.STATUS_COMPLETED);
        assertEquals("CANCELLED", Booking.STATUS_CANCELLED);
    }

    @Test
    @DisplayName("Should have correct delivery constants")
    void testDeliveryConstants() {
        assertEquals("PICKUP", Booking.DELIVERY_PICKUP);
        assertEquals("SHIPPING", Booking.DELIVERY_SHIPPING);
    }

    @Test
    @DisplayName("Default status should be PENDING_APPROVAL")
    void testDefaultStatus() {
        Booking booking = new Booking();
        
        assertEquals(Booking.STATUS_PENDING_APPROVAL, booking.getStatus());
    }

    @Test
    @DisplayName("Constructor should set PENDING_APPROVAL status")
    void testConstructorStatus() {
        User renter = new User();
        Product product = new Product();
        LocalDateTime bookingDate = LocalDateTime.now().plusDays(1);
        LocalDateTime returnDate = LocalDateTime.now().plusDays(3);
        
        Booking booking = new Booking(renter, product, bookingDate, returnDate, 100.0);
        
        assertEquals(Booking.STATUS_PENDING_APPROVAL, booking.getStatus());
    }

    @Test
    @DisplayName("Should set and get delivery method")
    void testDeliveryMethod() {
        Booking booking = new Booking();
        
        booking.setDeliveryMethod("PICKUP");
        
        assertEquals("PICKUP", booking.getDeliveryMethod());
    }

    @Test
    @DisplayName("Should set and get delivery code")
    void testDeliveryCode() {
        Booking booking = new Booking();
        
        booking.setDeliveryCode("ABC12345");
        
        assertEquals("ABC12345", booking.getDeliveryCode());
    }

    @Test
    @DisplayName("Should set and get pickup location")
    void testPickupLocation() {
        Booking booking = new Booking();
        
        booking.setPickupLocation("123 Main Street, City");
        
        assertEquals("123 Main Street, City", booking.getPickupLocation());
    }

    @Test
    @DisplayName("Should set and get rejection reason")
    void testRejectionReason() {
        Booking booking = new Booking();
        
        booking.setRejectionReason("Product not available");
        
        assertEquals("Product not available", booking.getRejectionReason());
    }

    @Test
    @DisplayName("Should set and get approved at timestamp")
    void testApprovedAt() {
        Booking booking = new Booking();
        LocalDateTime approvedTime = LocalDateTime.now();
        
        booking.setApprovedAt(approvedTime);
        
        assertEquals(approvedTime, booking.getApprovedAt());
    }

    @Test
    @DisplayName("Should set and get paid at timestamp")
    void testPaidAt() {
        Booking booking = new Booking();
        LocalDateTime paidTime = LocalDateTime.now();
        
        booking.setPaidAt(paidTime);
        
        assertEquals(paidTime, booking.getPaidAt());
    }

    @Test
    @DisplayName("Should handle complete approval workflow fields")
    void testCompleteApprovalWorkflow() {
        Booking booking = new Booking();
        LocalDateTime approvedTime = LocalDateTime.now();
        LocalDateTime paidTime = LocalDateTime.now().plusHours(1);
        
        // Approval phase
        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod("SHIPPING");
        booking.setApprovedAt(approvedTime);
        
        // Payment phase
        booking.setStatus(Booking.STATUS_PAID);
        booking.setDeliveryCode("XYZ12345");
        booking.setPaidAt(paidTime);
        
        assertEquals(Booking.STATUS_PAID, booking.getStatus());
        assertEquals("SHIPPING", booking.getDeliveryMethod());
        assertEquals("XYZ12345", booking.getDeliveryCode());
        assertEquals(approvedTime, booking.getApprovedAt());
        assertEquals(paidTime, booking.getPaidAt());
    }

    @Test
    @DisplayName("Should handle rejection workflow fields")
    void testRejectionWorkflow() {
        Booking booking = new Booking();
        
        booking.setStatus(Booking.STATUS_REJECTED);
        booking.setRejectionReason("Product is damaged");
        
        assertEquals(Booking.STATUS_REJECTED, booking.getStatus());
        assertEquals("Product is damaged", booking.getRejectionReason());
        assertNull(booking.getDeliveryMethod());
        assertNull(booking.getApprovedAt());
    }

    @Test
    @DisplayName("Should handle PICKUP delivery workflow")
    void testPickupDeliveryWorkflow() {
        Booking booking = new Booking();
        LocalDateTime approvedTime = LocalDateTime.now();
        LocalDateTime paidTime = LocalDateTime.now().plusHours(1);
        
        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod("PICKUP");
        booking.setPickupLocation("Store at 456 Oak Avenue");
        booking.setApprovedAt(approvedTime);
        
        booking.setStatus(Booking.STATUS_PAID);
        booking.setPaidAt(paidTime);
        
        assertEquals("PICKUP", booking.getDeliveryMethod());
        assertEquals("Store at 456 Oak Avenue", booking.getPickupLocation());
        assertNull(booking.getDeliveryCode()); // No code for pickup
    }

    @Test
    @DisplayName("Should allow null values for optional fields")
    void testNullOptionalFields() {
        Booking booking = new Booking();
        
        booking.setDeliveryMethod(null);
        booking.setDeliveryCode(null);
        booking.setPickupLocation(null);
        booking.setRejectionReason(null);
        booking.setApprovedAt(null);
        booking.setPaidAt(null);
        
        assertNull(booking.getDeliveryMethod());
        assertNull(booking.getDeliveryCode());
        assertNull(booking.getPickupLocation());
        assertNull(booking.getRejectionReason());
        assertNull(booking.getApprovedAt());
        assertNull(booking.getPaidAt());
    }

    @Test
    @DisplayName("Should maintain all existing fields alongside new fields")
    void testExistingFieldsIntact() {
        User renter = new User();
        renter.setId(1L);
        
        Product product = new Product();
        product.setId(2L);
        
        LocalDateTime bookingDate = LocalDateTime.now().plusDays(1);
        LocalDateTime returnDate = LocalDateTime.now().plusDays(3);
        
        Booking booking = new Booking(renter, product, bookingDate, returnDate, 150.0);
        booking.setId(10L);
        
        // Add new fields
        booking.setDeliveryMethod("SHIPPING");
        booking.setDeliveryCode("TEST1234");
        
        // Verify old fields still work
        assertEquals(10L, booking.getId());
        assertEquals(renter, booking.getRenter());
        assertEquals(product, booking.getProduct());
        assertEquals(bookingDate, booking.getBookingDate());
        assertEquals(returnDate, booking.getReturnDate());
        assertEquals(150.0, booking.getTotalPrice());
        
        // Verify new fields work
        assertEquals("SHIPPING", booking.getDeliveryMethod());
        assertEquals("TEST1234", booking.getDeliveryCode());
    }
}
