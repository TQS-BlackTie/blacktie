package tqs.blacktie.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.dto.ApproveBookingRequest;
import tqs.blacktie.dto.RejectBookingRequest;
import tqs.blacktie.dto.RequestDepositRequest;
import tqs.blacktie.service.BookingService;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public ResponseEntity<?> createBooking(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody BookingRequest request) {
        try {
            BookingResponse booking = bookingService.createBooking(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<BookingResponse>> getUserBookings(@PathVariable Long userId) {
        List<BookingResponse> bookings = bookingService.getUserBookings(userId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/user/{userId}/history")
    public ResponseEntity<List<BookingResponse>> getRenterHistory(@PathVariable Long userId) {
        try {
            List<BookingResponse> history = bookingService.getRenterHistory(userId);
            return ResponseEntity.ok(history);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/user/{userId}/active")
    public ResponseEntity<List<BookingResponse>> getActiveBookings(@PathVariable Long userId) {
        try {
            List<BookingResponse> activeBookings = bookingService.getActiveBookingsByRenter(userId);
            return ResponseEntity.ok(activeBookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        List<BookingResponse> bookings = bookingService.getAllBookings();
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getBookingsByProduct(
            @PathVariable Long productId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<BookingResponse> bookings = bookingService.getBookingsByProduct(productId, userId);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/owner/history")
    public ResponseEntity<List<BookingResponse>> getOwnerBookings(@RequestHeader("X-User-Id") Long ownerId) {
        List<BookingResponse> bookings = bookingService.getOwnerBookings(ownerId);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<?> getBookingById(@PathVariable Long bookingId) {
        try {
            BookingResponse booking = bookingService.getBookingById(bookingId);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{bookingId}")
    public ResponseEntity<?> cancelBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            bookingService.cancelBooking(bookingId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @GetMapping("/pending-approval")
    public ResponseEntity<?> getPendingApprovalBookings(@RequestHeader("X-User-Id") Long ownerId) {
        try {
            List<BookingResponse> bookings = bookingService.getPendingApprovalBookings(ownerId);
            return ResponseEntity.ok(bookings);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{bookingId}/approve")
    public ResponseEntity<?> approveBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long ownerId,
            @Valid @RequestBody ApproveBookingRequest request) {
        try {
            BookingResponse booking = bookingService.approveBooking(
                bookingId, ownerId, request.getDeliveryMethod(), request.getPickupLocation());
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PutMapping("/{bookingId}/reject")
    public ResponseEntity<?> rejectBooking(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long ownerId,
            @RequestBody RejectBookingRequest request) {
        try {
            BookingResponse booking = bookingService.rejectBooking(bookingId, ownerId, request.getReason());
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/payment")
    public ResponseEntity<?> processPayment(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            BookingResponse booking = bookingService.processPayment(bookingId, userId);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/request-deposit")
    public ResponseEntity<?> requestDeposit(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long ownerId,
            @Valid @RequestBody RequestDepositRequest request) {
        try {
            BookingResponse booking = bookingService.requestDeposit(bookingId, ownerId, request.getDepositAmount(), request.getReason());
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/{bookingId}/pay-deposit")
    public ResponseEntity<?> payDeposit(
            @PathVariable Long bookingId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            BookingResponse booking = bookingService.payDeposit(bookingId, userId);
            return ResponseEntity.ok(booking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }
}
