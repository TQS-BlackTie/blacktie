package tqs.blacktie.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests for Approval Flow")
class ApprovalFlowDTOTest {

    @Test
    @DisplayName("ApproveBookingRequest - should set and get delivery method")
    void testApproveBookingRequest_DeliveryMethod() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        
        request.setDeliveryMethod("PICKUP");
        
        assertEquals("PICKUP", request.getDeliveryMethod());
    }

    @Test
    @DisplayName("ApproveBookingRequest - should set and get pickup location")
    void testApproveBookingRequest_PickupLocation() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        
        request.setPickupLocation("123 Main Street");
        
        assertEquals("123 Main Street", request.getPickupLocation());
    }

    @Test
    @DisplayName("ApproveBookingRequest - should handle SHIPPING delivery")
    void testApproveBookingRequest_Shipping() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        
        request.setDeliveryMethod("SHIPPING");
        request.setPickupLocation(null);
        
        assertEquals("SHIPPING", request.getDeliveryMethod());
        assertNull(request.getPickupLocation());
    }

    @Test
    @DisplayName("RejectBookingRequest - should set and get reason")
    void testRejectBookingRequest_Reason() {
        RejectBookingRequest request = new RejectBookingRequest();
        
        request.setReason("Product not available");
        
        assertEquals("Product not available", request.getReason());
    }

    @Test
    @DisplayName("RejectBookingRequest - should handle null reason")
    void testRejectBookingRequest_NullReason() {
        RejectBookingRequest request = new RejectBookingRequest();
        
        request.setReason(null);
        
        assertNull(request.getReason());
    }

    @Test
    @DisplayName("RejectBookingRequest - should handle empty reason")
    void testRejectBookingRequest_EmptyReason() {
        RejectBookingRequest request = new RejectBookingRequest();
        
        request.setReason("");
        
        assertEquals("", request.getReason());
    }

    @Test
    @DisplayName("BookingResponse - should set and get delivery method")
    void testBookingResponse_DeliveryMethod() {
        BookingResponse response = new BookingResponse();
        
        response.setDeliveryMethod("PICKUP");
        
        assertEquals("PICKUP", response.getDeliveryMethod());
    }

    @Test
    @DisplayName("BookingResponse - should set and get delivery code")
    void testBookingResponse_DeliveryCode() {
        BookingResponse response = new BookingResponse();
        
        response.setDeliveryCode("ABC12345");
        
        assertEquals("ABC12345", response.getDeliveryCode());
    }

    @Test
    @DisplayName("BookingResponse - should set and get pickup location")
    void testBookingResponse_PickupLocation() {
        BookingResponse response = new BookingResponse();
        
        response.setPickupLocation("123 Main St");
        
        assertEquals("123 Main St", response.getPickupLocation());
    }

    @Test
    @DisplayName("BookingResponse - should set and get rejection reason")
    void testBookingResponse_RejectionReason() {
        BookingResponse response = new BookingResponse();
        
        response.setRejectionReason("Product unavailable");
        
        assertEquals("Product unavailable", response.getRejectionReason());
    }

    @Test
    @DisplayName("BookingResponse - should set and get approved at timestamp")
    void testBookingResponse_ApprovedAt() {
        BookingResponse response = new BookingResponse();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        response.setApprovedAt(now);
        
        assertEquals(now, response.getApprovedAt());
    }

    @Test
    @DisplayName("BookingResponse - should set and get paid at timestamp")
    void testBookingResponse_PaidAt() {
        BookingResponse response = new BookingResponse();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        response.setPaidAt(now);
        
        assertEquals(now, response.getPaidAt());
    }

    @Test
    @DisplayName("BookingResponse - should handle all new fields together")
    void testBookingResponse_AllNewFields() {
        BookingResponse response = new BookingResponse();
        java.time.LocalDateTime approvedTime = java.time.LocalDateTime.now();
        java.time.LocalDateTime paidTime = java.time.LocalDateTime.now().plusHours(1);
        
        response.setDeliveryMethod("SHIPPING");
        response.setDeliveryCode("XYZ98765");
        response.setPickupLocation(null);
        response.setRejectionReason(null);
        response.setApprovedAt(approvedTime);
        response.setPaidAt(paidTime);
        
        assertEquals("SHIPPING", response.getDeliveryMethod());
        assertEquals("XYZ98765", response.getDeliveryCode());
        assertNull(response.getPickupLocation());
        assertNull(response.getRejectionReason());
        assertEquals(approvedTime, response.getApprovedAt());
        assertEquals(paidTime, response.getPaidAt());
    }
}
