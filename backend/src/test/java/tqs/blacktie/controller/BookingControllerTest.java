package tqs.blacktie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.dto.RequestDepositRequest;
import tqs.blacktie.service.BookingService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingController Tests")
class BookingControllerTest {

    @Mock
    private BookingService bookingService;

    @InjectMocks
    private BookingController bookingController;

    private BookingRequest testRequest;
    private BookingResponse testResponse;

    @BeforeEach
    void setUp() {
        LocalDateTime bookingDate = LocalDateTime.now().plusDays(1);
        LocalDateTime returnDate = LocalDateTime.now().plusDays(3);

        testRequest = new BookingRequest(1L, bookingDate, returnDate);

        testResponse = new BookingResponse(
            1L, 1L, "John Doe", 1L, "Tuxedo", 5L, "Owner Name",
            bookingDate, returnDate, 100.0, "ACTIVE"
        );
    }

    @Nested
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void shouldCreateBookingSuccessfully() {
            when(bookingService.createBooking(eq(1L), any(BookingRequest.class)))
                .thenReturn(testResponse);

            ResponseEntity<?> response = bookingController.createBooking(1L, testRequest);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof BookingResponse);
            BookingResponse body = (BookingResponse) response.getBody();
            assertEquals(1L, body.getId());
            verify(bookingService, times(1)).createBooking(eq(1L), any(BookingRequest.class));
        }

        @Test
        @DisplayName("Should return bad request when IllegalArgumentException thrown")
        void shouldReturnBadRequestOnIllegalArgumentException() {
            when(bookingService.createBooking(eq(1L), any(BookingRequest.class)))
                .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = bookingController.createBooking(1L, testRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("User not found", response.getBody());
        }

        @Test
        @DisplayName("Should return conflict when IllegalStateException thrown")
        void shouldReturnConflictOnIllegalStateException() {
            when(bookingService.createBooking(eq(1L), any(BookingRequest.class)))
                .thenThrow(new IllegalStateException("Product not available"));

            ResponseEntity<?> response = bookingController.createBooking(1L, testRequest);

            assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
            assertEquals("Product not available", response.getBody());
        }
    }

    @Nested
    @DisplayName("Get Bookings Tests")
    class GetBookingsTests {

        @Test
        @DisplayName("Should get user bookings")
        void shouldGetUserBookings() {
            List<BookingResponse> bookings = Arrays.asList(testResponse);
            when(bookingService.getUserBookings(1L)).thenReturn(bookings);

            ResponseEntity<List<BookingResponse>> response = bookingController.getUserBookings(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            assertEquals(1L, response.getBody().get(0).getId());
            verify(bookingService, times(1)).getUserBookings(1L);
        }

        @Test
        @DisplayName("Should get all bookings")
        void shouldGetAllBookings() {
            List<BookingResponse> bookings = Arrays.asList(testResponse);
            when(bookingService.getAllBookings()).thenReturn(bookings);

            ResponseEntity<List<BookingResponse>> response = bookingController.getAllBookings();

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(bookingService, times(1)).getAllBookings();
        }

        @Test
        @DisplayName("Should get booking by id")
        void shouldGetBookingById() {
            when(bookingService.getBookingById(1L)).thenReturn(testResponse);

            ResponseEntity<?> response = bookingController.getBookingById(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof BookingResponse);
            BookingResponse body = (BookingResponse) response.getBody();
            assertEquals(1L, body.getId());
            verify(bookingService, times(1)).getBookingById(1L);
        }

        @Test
        @DisplayName("Should return not found when booking not found")
        void shouldReturnNotFoundWhenBookingNotFound() {
            when(bookingService.getBookingById(1L))
                .thenThrow(new IllegalArgumentException("Booking not found"));

            ResponseEntity<?> response = bookingController.getBookingById(1L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should get bookings by product id")
        void shouldGetBookingsByProductId() {
            List<BookingResponse> bookings = Arrays.asList(testResponse);
            when(bookingService.getBookingsByProduct(1L, 1L)).thenReturn(bookings);

            ResponseEntity<?> response = bookingController.getBookingsByProduct(1L, 1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            List<BookingResponse> body = (List<BookingResponse>) response.getBody();
            assertEquals(1, body.size());
            verify(bookingService, times(1)).getBookingsByProduct(1L, 1L);
        }

        @Test
        @DisplayName("Should get owner booking history")
        void shouldGetOwnerBookingHistory() {
            List<BookingResponse> bookings = Arrays.asList(testResponse);
            when(bookingService.getOwnerBookings(10L)).thenReturn(bookings);

            ResponseEntity<List<BookingResponse>> response = bookingController.getOwnerBookings(10L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(1, response.getBody().size());
            verify(bookingService, times(1)).getOwnerBookings(10L);
        }

        @Test
        @DisplayName("Should return not found when product not found for bookings")
        void shouldReturnNotFoundWhenProductMissing() {
            when(bookingService.getBookingsByProduct(1L, 1L))
                .thenThrow(new IllegalArgumentException("Product not found"));

            ResponseEntity<?> response = bookingController.getBookingsByProduct(1L, 1L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when owner not authorized for product bookings")
        void shouldReturnForbiddenWhenOwnerUnauthorized() {
            when(bookingService.getBookingsByProduct(1L, 1L))
                .thenThrow(new IllegalStateException("User is not authorized to view bookings for this product"));

            ResponseEntity<?> response = bookingController.getBookingsByProduct(1L, 1L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("User is not authorized to view bookings for this product", response.getBody());
        }
    }

    @Nested
    @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void shouldCancelBookingSuccessfully() {
            doNothing().when(bookingService).cancelBooking(1L, 1L);

            ResponseEntity<?> response = bookingController.cancelBooking(1L, 1L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            verify(bookingService, times(1)).cancelBooking(1L, 1L);
        }

        @Test
        @DisplayName("Should return not found when booking not found")
        void shouldReturnNotFoundWhenBookingNotFound() {
            doThrow(new IllegalArgumentException("Booking not found"))
                .when(bookingService).cancelBooking(1L, 1L);

            ResponseEntity<?> response = bookingController.cancelBooking(1L, 1L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when user not authorized")
        void shouldReturnForbiddenWhenUserNotAuthorized() {
            doThrow(new IllegalStateException("User is not authorized"))
                .when(bookingService).cancelBooking(1L, 1L);

            ResponseEntity<?> response = bookingController.cancelBooking(1L, 1L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("User is not authorized", response.getBody());
        }

        @Test
        @DisplayName("Should allow owner cancel flow (service succeeds)")
        void shouldAllowOwnerCancelFlow() {
            doNothing().when(bookingService).cancelBooking(1L, 10L);

            ResponseEntity<?> response = bookingController.cancelBooking(1L, 10L);

            assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
            verify(bookingService, times(1)).cancelBooking(1L, 10L);
        }
    }

    @Nested
    @DisplayName("Renter History Tests")
    class RenterHistoryTests {

        @Test
        @DisplayName("Should return renter history successfully")
        void shouldReturnRenterHistorySuccessfully() {
            LocalDateTime pastBookingDate = LocalDateTime.now().minusDays(10);
            LocalDateTime pastReturnDate = LocalDateTime.now().minusDays(7);

            BookingResponse completedBooking = new BookingResponse(
                1L, 1L, "John Doe", 1L, "Tuxedo", 5L, "Owner Name",
                pastBookingDate, pastReturnDate, 150.0, "COMPLETED"
            );

            BookingResponse cancelledBooking = new BookingResponse(
                2L, 1L, "John Doe", 2L, "Suit", 5L, "Owner Name",
                pastBookingDate, pastReturnDate, 200.0, "CANCELLED"
            );

            List<BookingResponse> history = Arrays.asList(completedBooking, cancelledBooking);

            when(bookingService.getRenterHistory(1L)).thenReturn(history);

            ResponseEntity<?> response = bookingController.getRenterHistory(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof List);
            @SuppressWarnings("unchecked")
            List<BookingResponse> body = (List<BookingResponse>) response.getBody();
            assertEquals(2, body.size());
            assertEquals("COMPLETED", body.get(0).getStatus());
            assertEquals("CANCELLED", body.get(1).getStatus());
            verify(bookingService, times(1)).getRenterHistory(1L);
        }

        @Test
        @DisplayName("Should return empty list when no history")
        void shouldReturnEmptyListWhenNoHistory() {
            when(bookingService.getRenterHistory(1L)).thenReturn(Arrays.asList());

            ResponseEntity<?> response = bookingController.getRenterHistory(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof List);
            @SuppressWarnings("unchecked")
            List<BookingResponse> body = (List<BookingResponse>) response.getBody();
            assertTrue(body.isEmpty());
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        void shouldReturnNotFoundWhenUserDoesNotExist() {
            when(bookingService.getRenterHistory(999L))
                .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = bookingController.getRenterHistory(999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Active Bookings Tests")
    class ActiveBookingsTests {

        @Test
        @DisplayName("Should return active bookings successfully")
        void shouldReturnActiveBookingsSuccessfully() {
            LocalDateTime futureBookingDate = LocalDateTime.now().plusDays(1);
            LocalDateTime futureReturnDate = LocalDateTime.now().plusDays(3);

            BookingResponse activeBooking = new BookingResponse(
                1L, 1L, "John Doe", 1L, "Tuxedo", 5L, "Owner Name",
                futureBookingDate, futureReturnDate, 100.0, "ACTIVE"
            );

            List<BookingResponse> activeBookings = Arrays.asList(activeBooking);

            when(bookingService.getActiveBookingsByRenter(1L)).thenReturn(activeBookings);

            ResponseEntity<?> response = bookingController.getActiveBookings(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof List);
            @SuppressWarnings("unchecked")
            List<BookingResponse> body = (List<BookingResponse>) response.getBody();
            assertEquals(1, body.size());
            assertEquals("ACTIVE", body.get(0).getStatus());
            verify(bookingService, times(1)).getActiveBookingsByRenter(1L);
        }

        @Test
        @DisplayName("Should return empty list when no active bookings")
        void shouldReturnEmptyListWhenNoActiveBookings() {
            when(bookingService.getActiveBookingsByRenter(1L)).thenReturn(Arrays.asList());

            ResponseEntity<?> response = bookingController.getActiveBookings(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof List);
            @SuppressWarnings("unchecked")
            List<BookingResponse> body = (List<BookingResponse>) response.getBody();
            assertTrue(body.isEmpty());
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        void shouldReturnNotFoundWhenUserDoesNotExistForActiveBookings() {
            when(bookingService.getActiveBookingsByRenter(999L))
                .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = bookingController.getActiveBookings(999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Request Deposit Tests")
    class RequestDepositTests {

        @Test
        @DisplayName("Should request deposit successfully")
        void shouldRequestDepositSuccessfully() {
            RequestDepositRequest depositRequest = new RequestDepositRequest(50.0, "Damaged item");
            BookingResponse expectedResponse = new BookingResponse(
                1L, 1L, "John Doe", 1L, "Tuxedo", 5L, "Owner Name",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100.0, "COMPLETED"
            );
            expectedResponse.setDepositAmount(50.0);
            expectedResponse.setDepositRequested(true);
            expectedResponse.setDepositReason("Damaged item");

            when(bookingService.requestDeposit(eq(1L), eq(5L), eq(50.0), eq("Damaged item")))
                .thenReturn(expectedResponse);

            ResponseEntity<?> response = bookingController.requestDeposit(1L, 5L, depositRequest);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof BookingResponse);
            BookingResponse body = (BookingResponse) response.getBody();
            assertEquals(50.0, body.getDepositAmount());
            assertEquals(true, body.getDepositRequested());
            assertEquals("Damaged item", body.getDepositReason());
            verify(bookingService, times(1)).requestDeposit(eq(1L), eq(5L), eq(50.0), eq("Damaged item"));
        }

        @Test
        @DisplayName("Should return bad request when booking not found")
        void shouldReturnBadRequestWhenBookingNotFoundForDeposit() {
            RequestDepositRequest depositRequest = new RequestDepositRequest(50.0, "Damaged item");
            
            when(bookingService.requestDeposit(eq(999L), eq(5L), eq(50.0), eq("Damaged item")))
                .thenThrow(new IllegalArgumentException("Booking not found with id: 999"));

            ResponseEntity<?> response = bookingController.requestDeposit(999L, 5L, depositRequest);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Booking not found with id: 999", response.getBody());
        }

        @Test
        @DisplayName("Should return forbidden when user is not owner")
        void shouldReturnForbiddenWhenUserIsNotOwner() {
            RequestDepositRequest depositRequest = new RequestDepositRequest(50.0, "Damaged item");
            
            when(bookingService.requestDeposit(eq(1L), eq(999L), eq(50.0), eq("Damaged item")))
                .thenThrow(new IllegalStateException("User is not authorized to request deposit for this booking"));

            ResponseEntity<?> response = bookingController.requestDeposit(1L, 999L, depositRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("User is not authorized to request deposit for this booking", response.getBody());
        }

        @Test
        @DisplayName("Should return forbidden when booking not completed")
        void shouldReturnForbiddenWhenBookingNotCompleted() {
            RequestDepositRequest depositRequest = new RequestDepositRequest(50.0, "Damaged item");
            
            when(bookingService.requestDeposit(eq(1L), eq(5L), eq(50.0), eq("Damaged item")))
                .thenThrow(new IllegalStateException("Deposit can only be requested after the booking return date"));

            ResponseEntity<?> response = bookingController.requestDeposit(1L, 5L, depositRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("return date"));
        }

        @Test
        @DisplayName("Should return forbidden when deposit already requested")
        void shouldReturnForbiddenWhenDepositAlreadyRequested() {
            RequestDepositRequest depositRequest = new RequestDepositRequest(50.0, "Damaged item");
            
            when(bookingService.requestDeposit(eq(1L), eq(5L), eq(50.0), eq("Damaged item")))
                .thenThrow(new IllegalStateException("Deposit has already been requested for this booking"));

            ResponseEntity<?> response = bookingController.requestDeposit(1L, 5L, depositRequest);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("already been requested"));
        }
    }

    @Nested
    @DisplayName("Pay Deposit Tests")
    class PayDepositTests {

        @Test
        @DisplayName("Should pay deposit successfully")
        void shouldPayDepositSuccessfully() {
            BookingResponse expectedResponse = new BookingResponse(
                1L, 1L, "John Doe", 1L, "Tuxedo", 5L, "Owner Name",
                LocalDateTime.now(), LocalDateTime.now().plusDays(1), 100.0, "COMPLETED"
            );
            expectedResponse.setDepositAmount(50.0);
            expectedResponse.setDepositRequested(true);
            expectedResponse.setDepositPaid(true);
            expectedResponse.setDepositPaidAt(LocalDateTime.now());

            when(bookingService.payDeposit(eq(1L), eq(1L)))
                .thenReturn(expectedResponse);

            ResponseEntity<?> response = bookingController.payDeposit(1L, 1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody() instanceof BookingResponse);
            BookingResponse body = (BookingResponse) response.getBody();
            assertEquals(true, body.getDepositPaid());
            assertNotNull(body.getDepositPaidAt());
            verify(bookingService, times(1)).payDeposit(eq(1L), eq(1L));
        }

        @Test
        @DisplayName("Should return bad request when booking not found")
        void shouldReturnBadRequestWhenBookingNotFoundForPayment() {
            when(bookingService.payDeposit(eq(999L), eq(1L)))
                .thenThrow(new IllegalArgumentException("Booking not found with id: 999"));

            ResponseEntity<?> response = bookingController.payDeposit(999L, 1L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Booking not found with id: 999", response.getBody());
        }

        @Test
        @DisplayName("Should return forbidden when user is not renter")
        void shouldReturnForbiddenWhenUserIsNotRenter() {
            when(bookingService.payDeposit(eq(1L), eq(999L)))
                .thenThrow(new IllegalStateException("User is not authorized to pay deposit for this booking"));

            ResponseEntity<?> response = bookingController.payDeposit(1L, 999L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertEquals("User is not authorized to pay deposit for this booking", response.getBody());
        }

        @Test
        @DisplayName("Should return forbidden when deposit not requested")
        void shouldReturnForbiddenWhenDepositNotRequested() {
            when(bookingService.payDeposit(eq(1L), eq(1L)))
                .thenThrow(new IllegalStateException("No deposit has been requested for this booking"));

            ResponseEntity<?> response = bookingController.payDeposit(1L, 1L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("No deposit has been requested"));
        }

        @Test
        @DisplayName("Should return forbidden when deposit already paid")
        void shouldReturnForbiddenWhenDepositAlreadyPaid() {
            when(bookingService.payDeposit(eq(1L), eq(1L)))
                .thenThrow(new IllegalStateException("Deposit has already been paid for this booking"));

            ResponseEntity<?> response = bookingController.payDeposit(1L, 1L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            assertTrue(response.getBody().toString().contains("already been paid"));
        }
    }
}
