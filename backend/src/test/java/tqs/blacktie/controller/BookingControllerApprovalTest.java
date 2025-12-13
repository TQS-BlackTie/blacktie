package tqs.blacktie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tqs.blacktie.dto.ApproveBookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.dto.RejectBookingRequest;
import tqs.blacktie.service.BookingService;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingControllerApprovalTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private BookingResponse testBooking;

    @BeforeEach
    void setUp() {
        testBooking = new BookingResponse();
        testBooking.setId(1L);
        testBooking.setStatus("PENDING_APPROVAL");
    }

    @Test
    void testGetPendingApprovalBookings_Success() {
        when(bookingService.getPendingApprovalBookings(1L)).thenReturn(Arrays.asList(testBooking));

        ResponseEntity<?> response = bookingController.getPendingApprovalBookings(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).getPendingApprovalBookings(1L);
    }

    @Test
    void testGetPendingApprovalBookings_NotFound() {
        when(bookingService.getPendingApprovalBookings(1L))
            .thenThrow(new IllegalArgumentException("Not found"));

        ResponseEntity<?> response = bookingController.getPendingApprovalBookings(1L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void testGetPendingApprovalBookings_Forbidden() {
        when(bookingService.getPendingApprovalBookings(1L))
            .thenThrow(new IllegalStateException("Not authorized"));

        ResponseEntity<?> response = bookingController.getPendingApprovalBookings(1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testApproveBooking_Success() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        request.setDeliveryMethod("PICKUP");
        request.setPickupLocation("123 Main St");

        when(bookingService.approveBooking(anyLong(), anyLong(), anyString(), anyString()))
            .thenReturn(testBooking);

        ResponseEntity<?> response = bookingController.approveBooking(1L, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).approveBooking(1L, 1L, "PICKUP", "123 Main St");
    }

    @Test
    void testApproveBooking_BadRequest() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        request.setDeliveryMethod("PICKUP");

        when(bookingService.approveBooking(anyLong(), anyLong(), anyString(), any()))
            .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> response = bookingController.approveBooking(1L, 1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testApproveBooking_Forbidden() {
        ApproveBookingRequest request = new ApproveBookingRequest();
        request.setDeliveryMethod("PICKUP");

        when(bookingService.approveBooking(anyLong(), anyLong(), anyString(), any()))
            .thenThrow(new IllegalStateException("Not authorized"));

        ResponseEntity<?> response = bookingController.approveBooking(1L, 1L, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    void testRejectBooking_Success() {
        RejectBookingRequest request = new RejectBookingRequest();
        request.setReason("Not available");

        when(bookingService.rejectBooking(anyLong(), anyLong(), anyString()))
            .thenReturn(testBooking);

        ResponseEntity<?> response = bookingController.rejectBooking(1L, 1L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).rejectBooking(1L, 1L, "Not available");
    }

    @Test
    void testRejectBooking_BadRequest() {
        RejectBookingRequest request = new RejectBookingRequest();

        when(bookingService.rejectBooking(anyLong(), anyLong(), any()))
            .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> response = bookingController.rejectBooking(1L, 1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testProcessPayment_Success() {
        when(bookingService.processPayment(1L, 1L)).thenReturn(testBooking);

        ResponseEntity<?> response = bookingController.processPayment(1L, 1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(bookingService).processPayment(1L, 1L);
    }

    @Test
    void testProcessPayment_BadRequest() {
        when(bookingService.processPayment(anyLong(), anyLong()))
            .thenThrow(new IllegalArgumentException("Invalid"));

        ResponseEntity<?> response = bookingController.processPayment(1L, 1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void testProcessPayment_Forbidden() {
        when(bookingService.processPayment(anyLong(), anyLong()))
            .thenThrow(new IllegalStateException("Not authorized"));

        ResponseEntity<?> response = bookingController.processPayment(1L, 1L);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
