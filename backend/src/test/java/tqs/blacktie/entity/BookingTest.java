package tqs.blacktie.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Booking Entity Tests")
class BookingTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create booking with no-args constructor")
        void whenNoArgsConstructor_thenCreateBooking() {
            Booking booking = new Booking();

            assertNotNull(booking);
        }

        @Test
        @DisplayName("Should create booking with all-args constructor")
        void whenAllArgsConstructor_thenCreateBooking() {
            User renter = new User("John Doe", "john@example.com", "password");
            renter.setId(1L);
            
            Product product = new Product("Smoking", "Black", 80.0);
            product.setId(1L);
            
            LocalDateTime bookingDate = LocalDateTime.now();
            LocalDateTime returnDate = bookingDate.plusDays(3);
            Double totalPrice = 240.0;

            Booking booking = new Booking(renter, product, bookingDate, returnDate, totalPrice);

            assertEquals(renter, booking.getRenter());
            assertEquals(product, booking.getProduct());
            assertEquals(bookingDate, booking.getBookingDate());
            assertEquals(returnDate, booking.getReturnDate());
            assertEquals(totalPrice, booking.getTotalPrice());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void whenSetId_thenGetId() {
            Booking booking = new Booking();
            booking.setId(1L);

            assertEquals(1L, booking.getId());
        }

        @Test
        @DisplayName("Should get and set renter")
        void whenSetRenter_thenGetRenter() {
            Booking booking = new Booking();
            User renter = new User("John Doe", "john@example.com", "password");
            renter.setId(1L);
            
            booking.setRenter(renter);

            assertEquals(renter, booking.getRenter());
            assertEquals("John Doe", booking.getRenter().getName());
        }

        @Test
        @DisplayName("Should get and set product")
        void whenSetProduct_thenGetProduct() {
            Booking booking = new Booking();
            Product product = new Product("Smoking", "Black", 80.0);
            product.setId(1L);
            
            booking.setProduct(product);

            assertEquals(product, booking.getProduct());
            assertEquals("Smoking", booking.getProduct().getName());
        }

        @Test
        @DisplayName("Should get and set bookingDate")
        void whenSetBookingDate_thenGetBookingDate() {
            Booking booking = new Booking();
            LocalDateTime bookingDate = LocalDateTime.of(2024, 12, 1, 10, 0);
            
            booking.setBookingDate(bookingDate);

            assertEquals(bookingDate, booking.getBookingDate());
        }

        @Test
        @DisplayName("Should get and set returnDate")
        void whenSetReturnDate_thenGetReturnDate() {
            Booking booking = new Booking();
            LocalDateTime returnDate = LocalDateTime.of(2024, 12, 5, 10, 0);
            
            booking.setReturnDate(returnDate);

            assertEquals(returnDate, booking.getReturnDate());
        }

        @Test
        @DisplayName("Should get and set totalPrice")
        void whenSetTotalPrice_thenGetTotalPrice() {
            Booking booking = new Booking();
            booking.setTotalPrice(320.0);

            assertEquals(320.0, booking.getTotalPrice());
        }
    }

    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {

        @Test
        @DisplayName("Booking should reference user and product")
        void whenBookingCreated_thenRelationshipsWork() {
            User renter = new User("John Doe", "john@example.com", "password");
            renter.setId(1L);
            renter.setRole("renter");
            
            Product product = new Product("Smoking", "Black classic", 80.0);
            product.setId(1L);
            product.setAvailable(true);
            
            LocalDateTime bookingDate = LocalDateTime.now();
            LocalDateTime returnDate = bookingDate.plusDays(5);

            Booking booking = new Booking(renter, product, bookingDate, returnDate, 400.0);
            booking.setId(1L);

            assertNotNull(booking.getRenter());
            assertNotNull(booking.getProduct());
            assertEquals("renter", booking.getRenter().getRole());
            assertEquals("john@example.com", booking.getRenter().getEmail());
            assertEquals("Smoking", booking.getProduct().getName());
            assertTrue(booking.getProduct().getAvailable());
        }

        @Test
        @DisplayName("Should update renter reference")
        void whenUpdateRenter_thenReferenceUpdated() {
            Booking booking = new Booking();
            
            User renter1 = new User("John", "john@example.com", "pass");
            renter1.setId(1L);
            booking.setRenter(renter1);
            
            assertEquals("John", booking.getRenter().getName());
            
            User renter2 = new User("Jane", "jane@example.com", "pass");
            renter2.setId(2L);
            booking.setRenter(renter2);
            
            assertEquals("Jane", booking.getRenter().getName());
            assertEquals(2L, booking.getRenter().getId());
        }

        @Test
        @DisplayName("Should update product reference")
        void whenUpdateProduct_thenReferenceUpdated() {
            Booking booking = new Booking();
            
            Product product1 = new Product("Smoking", "Black", 80.0);
            product1.setId(1L);
            booking.setProduct(product1);
            
            assertEquals("Smoking", booking.getProduct().getName());
            
            Product product2 = new Product("Tuxedo", "Navy", 120.0);
            product2.setId(2L);
            booking.setProduct(product2);
            
            assertEquals("Tuxedo", booking.getProduct().getName());
            assertEquals(120.0, booking.getProduct().getPrice());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("All fields should be null initially")
        void whenNewBooking_thenAllFieldsAreNull() {
            Booking booking = new Booking();

            assertNull(booking.getId());
            assertNull(booking.getRenter());
            assertNull(booking.getProduct());
            assertNull(booking.getBookingDate());
            assertNull(booking.getReturnDate());
            assertNull(booking.getTotalPrice());
        }
    }
}
