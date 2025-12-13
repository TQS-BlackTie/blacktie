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

    private static final String ROLE_OWNER = "owner";

    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public BookingService(BookingRepository bookingRepository, 
                         ProductRepository productRepository,
                         UserRepository userRepository,
                         NotificationService notificationService) {
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
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

        // Check for overlapping bookings (only APPROVED and PAID bookings)
        List<Booking> overlappingBookings = bookingRepository
            .findByProductAndBookingDateLessThanEqualAndReturnDateGreaterThanEqual(
                product, request.getReturnDate(), request.getBookingDate())
            .stream()
            .filter(booking -> Booking.STATUS_APPROVED.equals(booking.getStatus()) 
                            || Booking.STATUS_PAID.equals(booking.getStatus()))
            .toList();

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

        // Create notification for product owner
        if (product.getOwner() != null) {
            notificationService.createNewBookingNotification(product.getOwner(), savedBooking);
        }

        return convertToResponse(savedBooking);
    }

    public List<BookingResponse> getUserBookings(Long userId) {
        List<Booking> bookings = bookingRepository.findByRenterId(userId);
        return bookings.stream()
            .filter(booking -> !Booking.STATUS_COMPLETED.equals(booking.getStatus()) 
                            && !Booking.STATUS_CANCELLED.equals(booking.getStatus())
                            && !Booking.STATUS_REJECTED.equals(booking.getStatus()))
            .map(this::convertToResponse)
            .toList();
    }

    public List<BookingResponse> getOwnerBookings(Long ownerId) {
        // Get all bookings for products owned by the owner, but return only past/completed or cancelled bookings
        List<Booking> bookings = bookingRepository.findByProductOwnerId(ownerId);
        return bookings.stream()
            .filter(booking -> Booking.STATUS_COMPLETED.equals(booking.getStatus()) || Booking.STATUS_CANCELLED.equals(booking.getStatus()))
            .map(this::convertToResponse)
            .collect(Collectors.toList());
    }

    public List<BookingResponse> getAllBookings() {
        List<Booking> bookings = bookingRepository.findAll();
        return bookings.stream()
            .map(this::convertToResponse)
            .toList();
    }

    public List<BookingResponse> getBookingsByProduct(Long productId, Long requesterId) {
        User requester = userRepository.findById(requesterId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + requesterId));

        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found with id: " + productId));

        if (ROLE_OWNER.equalsIgnoreCase(requester.getRole())
            && product.getOwner() != null
            && !product.getOwner().getId().equals(requester.getId())) {
            throw new IllegalStateException("User is not authorized to view bookings for this product");
        }

        List<Booking> bookings = bookingRepository.findByProduct(product);
        return bookings.stream()
            .filter(booking -> Booking.STATUS_PAID.equals(booking.getStatus()) || 
                             Booking.STATUS_COMPLETED.equals(booking.getStatus()))
            .map(this::convertToResponse)
            .toList();
    }

    public BookingResponse getBookingById(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));
        return convertToResponse(booking);
    }

    public void cancelBooking(Long bookingId, Long userId) {
        User requester = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Check if booking is already cancelled
        if (Booking.STATUS_CANCELLED.equals(booking.getStatus())) {
            throw new IllegalArgumentException("Booking not found with id: " + bookingId);
        }

        boolean isRenter = booking.getRenter().getId().equals(userId);
        boolean isOwner = ROLE_OWNER.equalsIgnoreCase(requester.getRole()) &&
            booking.getProduct().getOwner() != null &&
            booking.getProduct().getOwner().getId().equals(requester.getId());

        // Verify the booking belongs to the user or requester is an owner (managing products)
        if (!isRenter && !isOwner) {
            throw new IllegalStateException("User is not authorized to cancel this booking");
        }

        // Only allow cancellation if booking hasn't started yet
        if (booking.getBookingDate().isBefore(LocalDateTime.now())) {
            throw new IllegalStateException("Cannot cancel a booking that has already started");
        }

        // Update status to CANCELLED instead of deleting
        booking.setStatus(Booking.STATUS_CANCELLED);
        bookingRepository.save(booking);

        // Create notification based on who cancelled
        if (isRenter && booking.getProduct().getOwner() != null) {
            // Renter cancelled - notify owner
            notificationService.createBookingCancelledByRenterNotification(booking.getProduct().getOwner(), booking);
        } else if (isOwner) {
            // Owner cancelled - notify renter
            notificationService.createBookingCancelledByOwnerNotification(booking.getRenter(), booking);
        }
    }

    public List<BookingResponse> getRenterHistory(Long userId) {
        // Verify user exists
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Get all bookings for the renter (COMPLETED and CANCELLED)
        List<Booking> allBookings = bookingRepository.findByRenterId(userId);
        
        return allBookings.stream()
            .filter(booking -> Booking.STATUS_COMPLETED.equals(booking.getStatus()) || Booking.STATUS_CANCELLED.equals(booking.getStatus()))
            .map(this::convertToResponse)
            .toList();
    }

    public List<BookingResponse> getPendingApprovalBookings(Long ownerId) {
        // Verify user is owner
        User owner = userRepository.findById(ownerId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + ownerId));
        
        if (!"owner".equalsIgnoreCase(owner.getRole())) {
            throw new IllegalStateException("User is not an owner");
        }

        List<Booking> bookings = bookingRepository.findByProductOwnerId(ownerId);
        return bookings.stream()
            .filter(booking -> Booking.STATUS_PENDING_APPROVAL.equals(booking.getStatus()))
    public List<BookingResponse> getActiveBookingsByRenter(Long userId) {
        // Verify user exists
        userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));

        // Get all bookings for the renter with ACTIVE status
        List<Booking> allBookings = bookingRepository.findByRenterId(userId);
        
        return allBookings.stream()
            .filter(booking -> Booking.STATUS_ACTIVE.equals(booking.getStatus()))
            .map(this::convertToResponse)
            .toList();
    }

    public BookingResponse approveBooking(Long bookingId, Long ownerId, String deliveryMethod, String pickupLocation) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify the owner owns the product
        if (booking.getProduct().getOwner() == null || 
            !booking.getProduct().getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("User is not authorized to approve this booking");
        }

        // Verify booking is in pending approval status
        if (!Booking.STATUS_PENDING_APPROVAL.equals(booking.getStatus())) {
            throw new IllegalStateException("Booking is not pending approval");
        }

        // Validate delivery method
        if (!Booking.DELIVERY_PICKUP.equals(deliveryMethod) && !Booking.DELIVERY_SHIPPING.equals(deliveryMethod)) {
            throw new IllegalArgumentException("Invalid delivery method. Must be PICKUP or SHIPPING");
        }

        // If PICKUP, require pickup location
        if (Booking.DELIVERY_PICKUP.equals(deliveryMethod) && (pickupLocation == null || pickupLocation.trim().isEmpty())) {
            throw new IllegalArgumentException("Pickup location is required for PICKUP delivery method");
        }

        booking.setStatus(Booking.STATUS_APPROVED);
        booking.setDeliveryMethod(deliveryMethod);
        booking.setPickupLocation(pickupLocation);
        booking.setApprovedAt(LocalDateTime.now());

        Booking savedBooking = bookingRepository.save(booking);

        // Notify renter that booking was approved
        notificationService.createBookingApprovedNotification(booking.getRenter(), savedBooking);

        return convertToResponse(savedBooking);
    }

    public BookingResponse rejectBooking(Long bookingId, Long ownerId, String reason) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify the owner owns the product
        if (booking.getProduct().getOwner() == null || 
            !booking.getProduct().getOwner().getId().equals(ownerId)) {
            throw new IllegalStateException("User is not authorized to reject this booking");
        }

        // Verify booking is in pending approval status
        if (!Booking.STATUS_PENDING_APPROVAL.equals(booking.getStatus())) {
            throw new IllegalStateException("Booking is not pending approval");
        }

        booking.setStatus(Booking.STATUS_REJECTED);
        booking.setRejectionReason(reason);

        Booking savedBooking = bookingRepository.save(booking);

        // Notify renter that booking was rejected
        notificationService.createBookingRejectedNotification(booking.getRenter(), savedBooking, reason);

        return convertToResponse(savedBooking);
    }

    public BookingResponse processPayment(Long bookingId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId)
            .orElseThrow(() -> new IllegalArgumentException("Booking not found with id: " + bookingId));

        // Verify user is the renter
        if (!booking.getRenter().getId().equals(userId)) {
            throw new IllegalStateException("User is not authorized to pay for this booking");
        }

        // Verify booking is approved
        if (!Booking.STATUS_APPROVED.equals(booking.getStatus())) {
            throw new IllegalStateException("Booking must be approved before payment");
        }

        booking.setStatus(Booking.STATUS_PAID);
        booking.setPaidAt(LocalDateTime.now());

        // Generate delivery code if shipping
        if (Booking.DELIVERY_SHIPPING.equals(booking.getDeliveryMethod())) {
            booking.setDeliveryCode(generateDeliveryCode());
        }

        Booking savedBooking = bookingRepository.save(booking);

        // Notify owner that payment was received
        if (booking.getProduct().getOwner() != null) {
            notificationService.createPaymentReceivedNotification(booking.getProduct().getOwner(), savedBooking);
        }

        return convertToResponse(savedBooking);
    }

    private String generateDeliveryCode() {
        // Generate a random 8-character alphanumeric code
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private BookingResponse convertToResponse(Booking booking) {
        Long ownerId = booking.getProduct().getOwner() != null ? booking.getProduct().getOwner().getId() : null;
        String ownerName = booking.getProduct().getOwner() != null ? booking.getProduct().getOwner().getName() : "Unknown";
        
        BookingResponse response = new BookingResponse(
            booking.getId(),
            booking.getRenter().getId(),
            booking.getRenter().getName(),
            booking.getProduct().getId(),
            booking.getProduct().getName(),
            ownerId,
            ownerName,
            booking.getBookingDate(),
            booking.getReturnDate(),
            booking.getTotalPrice(),
            booking.getStatus()
        );
        
        response.setDeliveryMethod(booking.getDeliveryMethod());
        response.setDeliveryCode(booking.getDeliveryCode());
        response.setPickupLocation(booking.getPickupLocation());
        response.setRejectionReason(booking.getRejectionReason());
        response.setApprovedAt(booking.getApprovedAt());
        response.setPaidAt(booking.getPaidAt());
        
        return response;
    }
}
