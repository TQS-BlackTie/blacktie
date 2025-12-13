package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BookingService Tests")
class BookingServiceTest {

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

    private User testUser;
    private Product testProduct;
    private Booking testBooking;
    private BookingRequest testRequest;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "password123");
        testUser.setId(1L);

        testProduct = new Product("Tuxedo", "Classic black tuxedo", 50.0);
        testProduct.setId(1L);
        testProduct.setAvailable(true);

        LocalDateTime bookingDate = LocalDateTime.now().plusDays(1);
        LocalDateTime returnDate = LocalDateTime.now().plusDays(3);

        testRequest = new BookingRequest(1L, bookingDate, returnDate);

        testBooking = new Booking(testUser, testProduct, bookingDate, returnDate, 100.0);
        testBooking.setId(1L);
    }

    @Nested
    @DisplayName("Create Booking Tests")
    class CreateBookingTests {

        @Test
        @DisplayName("Should create booking successfully")
        void shouldCreateBookingSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(bookingRepository.findByProductAndBookingDateLessThanEqualAndReturnDateGreaterThanEqual(
                any(), any(), any())).thenReturn(Collections.emptyList());
            when(bookingRepository.save(any(Booking.class))).thenReturn(testBooking);

            BookingResponse response = bookingService.createBooking(1L, testRequest);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals(1L, response.getRenterId());
            assertEquals(1L, response.getProductId());
            assertEquals(100.0, response.getTotalPrice());
            verify(bookingRepository, times(1)).save(any(Booking.class));
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, testRequest));

            assertEquals("User not found with id: 1", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when product not found")
        void shouldThrowExceptionWhenProductNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, testRequest));

            assertEquals("Product not found with id: 1", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when product not available")
        void shouldThrowExceptionWhenProductNotAvailable() {
            testProduct.setAvailable(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(1L, testRequest));

            assertEquals("Product is not available for booking", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when return date is before booking date")
        void shouldThrowExceptionWhenReturnDateBeforeBookingDate() {
            LocalDateTime bookingDate = LocalDateTime.now().plusDays(3);
            LocalDateTime returnDate = LocalDateTime.now().plusDays(1);
            BookingRequest invalidRequest = new BookingRequest(1L, bookingDate, returnDate);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, invalidRequest));

            assertEquals("Return date must be after booking date", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when booking date is in the past")
        void shouldThrowExceptionWhenBookingDateInPast() {
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            LocalDateTime futureDate = LocalDateTime.now().plusDays(1);
            BookingRequest pastRequest = new BookingRequest(1L, pastDate, futureDate);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.createBooking(1L, pastRequest));

            assertEquals("Booking date cannot be in the past", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when product already booked for dates")
        void shouldThrowExceptionWhenProductAlreadyBooked() {
            testBooking.setStatus(Booking.STATUS_APPROVED); // Only APPROVED/PAID bookings block overlaps
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(bookingRepository.findByProductAndBookingDateLessThanEqualAndReturnDateGreaterThanEqual(
                any(), any(), any())).thenReturn(Arrays.asList(testBooking));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.createBooking(1L, testRequest));

            assertEquals("Product is already booked for the selected dates", exception.getMessage());
            verify(bookingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Bookings Tests")
    class GetBookingsTests {

        @Test
        @DisplayName("Should get user bookings")
        void shouldGetUserBookings() {
            when(bookingRepository.findByRenterId(1L)).thenReturn(Arrays.asList(testBooking));

            List<BookingResponse> responses = bookingService.getUserBookings(1L);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(1L, responses.get(0).getId());
            verify(bookingRepository, times(1)).findByRenterId(1L);
        }

        @Test
        @DisplayName("Should get all bookings")
        void shouldGetAllBookings() {
            when(bookingRepository.findAll()).thenReturn(Arrays.asList(testBooking));

            List<BookingResponse> responses = bookingService.getAllBookings();

            assertNotNull(responses);
            assertEquals(1, responses.size());
            verify(bookingRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("Should get bookings by product")
        void shouldGetBookingsByProduct() {
            User owner = new User("Owner", "owner@example.com", "pass", "owner");
            owner.setId(10L);
            testProduct.setOwner(owner);
            testBooking.setStatus(Booking.STATUS_PAID);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));
            when(bookingRepository.findByProduct(testProduct)).thenReturn(Arrays.asList(testBooking));

            List<BookingResponse> responses = bookingService.getBookingsByProduct(1L, 10L);

            assertNotNull(responses);
            assertEquals(1, responses.size());
            assertEquals(1L, responses.get(0).getProductId());
            verify(productRepository, times(1)).findById(1L);
            verify(bookingRepository, times(1)).findByProduct(testProduct);
        }

        @Test
        @DisplayName("Should throw exception when product not found while fetching bookings")
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBookingsByProduct(99L, 1L));

            assertEquals("Product not found with id: 99", exception.getMessage());
            verify(bookingRepository, never()).findByProduct(any());
        }

        @Test
        @DisplayName("Should forbid owner from fetching bookings of another owner's product")
        void shouldForbidOwnerOnOtherProduct() {
            User owner = new User("Owner", "owner@example.com", "pass", "owner");
            owner.setId(10L);
            User otherOwner = new User("Other", "o@example.com", "pass", "owner");
            otherOwner.setId(20L);
            testProduct.setOwner(otherOwner);

            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.getBookingsByProduct(1L, 10L));

            assertEquals("User is not authorized to view bookings for this product", exception.getMessage());
            verify(bookingRepository, never()).findByProduct(any());
        }

        @Test
        @DisplayName("Renter can view bookings for product")
        void renterCanViewBookingsForProduct() {
            testBooking.setStatus(Booking.STATUS_PAID);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser)); // renter
            when(bookingRepository.findByProduct(testProduct)).thenReturn(Arrays.asList(testBooking));

            List<BookingResponse> responses = bookingService.getBookingsByProduct(1L, 1L);

            assertEquals(1, responses.size());
            verify(bookingRepository, times(1)).findByProduct(testProduct);
        }

        @Test
        @DisplayName("Should get booking by id")
        void shouldGetBookingById() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));

            BookingResponse response = bookingService.getBookingById(1L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            verify(bookingRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("Should throw exception when booking not found by id")
        void shouldThrowExceptionWhenBookingNotFoundById() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.getBookingById(1L));

            assertEquals("Booking not found with id: 1", exception.getMessage());
        }
    }

    @Nested
        @DisplayName("Cancel Booking Tests")
    class CancelBookingTests {

        @Test
        @DisplayName("Should cancel booking successfully")
        void shouldCancelBookingSuccessfully() {
            testUser.setRole("renter");
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            bookingService.cancelBooking(1L, 1L);

            verify(bookingRepository, times(1)).save(testBooking);
            assertEquals("CANCELLED", testBooking.getStatus());
        }

        @Test
        @DisplayName("Should throw exception when booking not found")
        void shouldThrowExceptionWhenBookingNotFound() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.empty());
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelBooking(1L, 1L));

            assertEquals("Booking not found with id: 1", exception.getMessage());
            verify(bookingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> bookingService.cancelBooking(1L, 1L));

            assertEquals("User not found with id: 1", exception.getMessage());
            verify(bookingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when user not authorized")
        void shouldThrowExceptionWhenUserNotAuthorized() {
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(999L)).thenReturn(Optional.of(testUser));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, 999L));

            assertEquals("User is not authorized to cancel this booking", exception.getMessage());
            verify(bookingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should allow owner to cancel own product booking")
        void shouldAllowOwnerToCancelOwnProductBooking() {
            User owner = new User("Owner", "owner@example.com", "pass", "owner");
            owner.setId(10L);
            testProduct.setOwner(owner);
            testBooking.setProduct(testProduct);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            bookingService.cancelBooking(1L, 10L);

            verify(bookingRepository, times(1)).save(testBooking);
            assertEquals("CANCELLED", testBooking.getStatus());
        }

        @Test
        @DisplayName("Owner cannot cancel booking for product they do not own")
        void ownerCannotCancelOthersProductBooking() {
            User owner = new User("Owner", "owner@example.com", "pass", "owner");
            owner.setId(10L);
            User otherOwner = new User("Other", "other@example.com", "pass", "owner");
            otherOwner.setId(20L);
            testProduct.setOwner(otherOwner);

            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, 10L));

            assertEquals("User is not authorized to cancel this booking", ex.getMessage());
            verify(bookingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception when booking already started")
        void shouldThrowExceptionWhenBookingAlreadyStarted() {
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            testBooking.setBookingDate(pastDate);
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> bookingService.cancelBooking(1L, 1L));

            assertEquals("Cannot cancel a booking that has already started", exception.getMessage());
            verify(bookingRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should allow owner to cancel another user's booking")
        void shouldAllowOwnerToCancelOthersBooking() {
            User owner = new User("Owner", "owner@example.com", "pass", "owner");
            owner.setId(10L);
            testProduct.setOwner(owner); // Set the owner of the product
            
            when(bookingRepository.findById(1L)).thenReturn(Optional.of(testBooking));
            when(userRepository.findById(10L)).thenReturn(Optional.of(owner));

            bookingService.cancelBooking(1L, 10L);

            verify(bookingRepository, times(1)).save(testBooking);
            assertEquals("CANCELLED", testBooking.getStatus());
        }
    }

    @Nested
    @DisplayName("Renter History Tests")
    class RenterHistoryTests {

        @Test
        @DisplayName("Should return completed and cancelled bookings only")
        void shouldReturnCompletedAndCancelledBookingsOnly() {
            LocalDateTime bookingDate = LocalDateTime.now().minusDays(10);
            LocalDateTime returnDate = LocalDateTime.now().minusDays(7);

            Booking completedBooking = new Booking(testUser, testProduct, bookingDate, returnDate, 150.0);
            completedBooking.setId(1L);
            completedBooking.setStatus("COMPLETED");

            Booking cancelledBooking = new Booking(testUser, testProduct, bookingDate, returnDate, 200.0);
            cancelledBooking.setId(2L);
            cancelledBooking.setStatus("CANCELLED");

            Booking activeBooking = new Booking(testUser, testProduct, 
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(3), 100.0);
            activeBooking.setId(3L);
            activeBooking.setStatus("ACTIVE");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findByRenterId(1L))
                .thenReturn(Arrays.asList(completedBooking, cancelledBooking, activeBooking));

            List<BookingResponse> history = bookingService.getRenterHistory(1L);

            assertEquals(2, history.size());
            assertTrue(history.stream().anyMatch(b -> b.getId().equals(1L) && "COMPLETED".equals(b.getStatus())));
            assertTrue(history.stream().anyMatch(b -> b.getId().equals(2L) && "CANCELLED".equals(b.getStatus())));
            assertFalse(history.stream().anyMatch(b -> b.getId().equals(3L)));
        }

        @Test
        @DisplayName("Should return empty list when no history exists")
        void shouldReturnEmptyListWhenNoHistoryExists() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findByRenterId(1L)).thenReturn(Collections.emptyList());

            List<BookingResponse> history = bookingService.getRenterHistory(1L);

            assertTrue(history.isEmpty());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> {
                bookingService.getRenterHistory(999L);
            });
        }

        @Test
        @DisplayName("Should include owner information in history")
        void shouldIncludeOwnerInformationInHistory() {
            User owner = new User("Owner Name", "owner@example.com", "pass", "owner");
            owner.setId(5L);
            testProduct.setOwner(owner);

            LocalDateTime bookingDate = LocalDateTime.now().minusDays(10);
            LocalDateTime returnDate = LocalDateTime.now().minusDays(7);

            Booking completedBooking = new Booking(testUser, testProduct, bookingDate, returnDate, 150.0);
            completedBooking.setId(1L);
            completedBooking.setStatus("COMPLETED");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findByRenterId(1L)).thenReturn(Arrays.asList(completedBooking));

            List<BookingResponse> history = bookingService.getRenterHistory(1L);

            assertEquals(1, history.size());
            BookingResponse response = history.get(0);
            assertEquals(5L, response.getOwnerId());
            assertEquals("Owner Name", response.getOwnerName());
        }

        @Test
        @DisplayName("Should handle bookings with null owner")
        void shouldHandleBookingsWithNullOwner() {
            testProduct.setOwner(null);

            LocalDateTime bookingDate = LocalDateTime.now().minusDays(10);
            LocalDateTime returnDate = LocalDateTime.now().minusDays(7);

            Booking completedBooking = new Booking(testUser, testProduct, bookingDate, returnDate, 150.0);
            completedBooking.setId(1L);
            completedBooking.setStatus("COMPLETED");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(bookingRepository.findByRenterId(1L)).thenReturn(Arrays.asList(completedBooking));

            List<BookingResponse> history = bookingService.getRenterHistory(1L);

            assertEquals(1, history.size());
            BookingResponse response = history.get(0);
            assertNull(response.getOwnerId());
            assertEquals("Unknown", response.getOwnerName());
        }
    }
}
