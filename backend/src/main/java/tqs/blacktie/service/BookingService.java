package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.dto.BookingRequest;
import tqs.blacktie.dto.BookingResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public BookingService(BookingRepository bookingRepository, 
                         ProductRepository productRepository,
                         UserRepository userRepository) {
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public BookingResponse createBooking(Long userId, BookingRequest request) {
        // Validate user exists
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Validate product exists
        Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + request.getProductId()));

        // Validate product is available
        if (product.getAvailable() == null || !product.getAvailable()) {
            throw new IllegalStateException("Product is not available for booking");
        }

        // Validate dates
        if (request.getReturnDate().isBefore(request.getBookingDate())) {
            throw new IllegalArgumentException("Return date must be after booking date");
        }

        if (request.getBookingDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Booking date cannot be in the past");
        }

        // Check for overlapping bookings
        List<Booking> overlappingBookings = bookingRepository
            .findByProductAndBookingDateLessThanEqualAndReturnDateGreaterThanEqual(
                product, request.getReturnDate(), request.getBookingDate());

        if (!overlappingBookings.isEmpty()) {
            throw new IllegalStateException("Product is already booked for the selected dates");
        }

        // Calculate total price (price per day * number of days)
        long days = ChronoUnit.DAYS.between(request.getBookingDate(), request.getReturnDate());
        if (days == 0) {
            days = 1; // Minimum 1 day
        }
        Double totalPrice = product.getPrice() * days;

        // Create booking
        Booking booking = new Booking(user, product, request.getBookingDate(), request.getReturnDate(), totalPrice);
        Booking savedBooking = bookingRepository.save(booking);

        return convertToResponse(savedBooking);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByRenterId(userId);
        return bookings.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        return convertToResponse(booking);
    }

    public void cancelBooking(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify the booking belongs to the user
        if (!booking.getRenter().getId().equals(userId)) {
            throw new IllegalStateException("User is not authorized to cancel this booking");
        }

        // Only allow cancellation if booking hasn't started yet
        if (booking.getBookingDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot cancel a booking that has already started");
        }

        bookingRepository.delete(booking);
    }

    private BookingResponse convertToResponse(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getRenter().getId(),
            booking.getRenter().getName(),
            booking.getProduct().getId(),
            booking.getProduct().getName(),
            booking.getBookingDate(),
            booking.getReturnDate(),
            booking.getTotalPrice()
        );
    }
}
