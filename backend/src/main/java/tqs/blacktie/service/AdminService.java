package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.dto.AdminUserResponse;
import tqs.blacktie.dto.PlatformMetricsResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Notification;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;

@Service
public class AdminService {

    private static final String USER_NOT_FOUND_MSG = "User not found";

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    public AdminService(UserRepository userRepository,
                       BookingRepository bookingRepository,
                       ProductRepository productRepository,
                       NotificationService notificationService,
                       NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
    }

    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
            .map(user -> User.ROLE_ADMIN.equals(user.getRole()))
            .orElse(false);
    }

    public PlatformMetricsResponse getPlatformMetrics() {
        long totalUsers = userRepository.count() - 1; // Exclude admin
        long totalOwners = userRepository.countByRole(User.ROLE_OWNER);
        long totalRenters = userRepository.countByRole(User.ROLE_RENTER);
        
        long totalProducts = productRepository.count();
        long availableProducts = productRepository.countByAvailableTrue();
        
        long totalBookings = bookingRepository.count();
        long activeBookings = bookingRepository.countByStatus(Booking.STATUS_PAID);
        long completedBookings = bookingRepository.countByStatus(Booking.STATUS_COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(Booking.STATUS_CANCELLED);
        
        Double totalRevenueResult = bookingRepository.sumTotalPriceByStatus(Booking.STATUS_COMPLETED);
        double totalRevenue = totalRevenueResult != null ? totalRevenueResult : 0.0;
        double averageBookingValue = completedBookings > 0 ? totalRevenue / completedBookings : 0;
        
        PlatformMetricsResponse response = new PlatformMetricsResponse();
        response.setTotalUsers(totalUsers);
        response.setTotalOwners(totalOwners);
        response.setTotalRenters(totalRenters);
        response.setTotalProducts(totalProducts);
        response.setAvailableProducts(availableProducts);
        response.setTotalBookings(totalBookings);
        response.setActiveBookings(activeBookings);
        response.setCompletedBookings(completedBookings);
        response.setCancelledBookings(cancelledBookings);
        response.setTotalRevenue(totalRevenue);
        response.setAverageBookingValue(averageBookingValue);
        return response;
    }

    public List<AdminUserResponse> getAllUsersForAdmin() {
        return userRepository.findByRoleNot(User.ROLE_ADMIN).stream()
            .map(this::mapUserToAdminResponse)
            .toList();
    }

    private AdminUserResponse mapUserToAdminResponse(User user) {
        long bookingsCount = bookingRepository.countByRenterId(user.getId());
        long productsCount = productRepository.countByOwnerId(user.getId());
        
        AdminUserResponse response = new AdminUserResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());
        response.setStatus(user.getStatus() != null ? user.getStatus() : User.STATUS_ACTIVE);
        response.setPhone(user.getPhone());
        response.setAddress(user.getAddress());
        response.setBusinessInfo(user.getBusinessInfo());
        response.setCreatedAt(user.getCreatedAt().toString());
        response.setBookingsCount(bookingsCount);
        response.setProductsCount(productsCount);
        return response;
    }

    public User updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot modify admin user status");
        }

        String normalizedStatus = status.toLowerCase().trim();
        if (!normalizedStatus.equals(User.STATUS_ACTIVE) && 
            !normalizedStatus.equals(User.STATUS_SUSPENDED) && 
            !normalizedStatus.equals(User.STATUS_BANNED)) {
            throw new IllegalArgumentException("Invalid status. Only 'active', 'suspended', or 'banned' are allowed");
        }

        String previousStatus = user.getStatus();
        user.setStatus(normalizedStatus);
        User savedUser = userRepository.save(user);

        // Handle status change notifications and booking cancellations
        if (normalizedStatus.equals(User.STATUS_SUSPENDED) || normalizedStatus.equals(User.STATUS_BANNED)) {
            // Notify the user about their account status
            if (normalizedStatus.equals(User.STATUS_SUSPENDED)) {
                notificationService.createAccountSuspendedNotification(user);
            } else {
                notificationService.createAccountBannedNotification(user);
            }

            // Cancel all active bookings for products owned by this user
            cancelBookingsForOwnerProducts(user, "Owner account " + normalizedStatus);

            // Cancel all active bookings made by this user as a renter
            cancelUserBookings(user, "Renter account " + normalizedStatus);
        } else if (normalizedStatus.equals(User.STATUS_ACTIVE) && 
                   (previousStatus.equals(User.STATUS_SUSPENDED) || previousStatus.equals(User.STATUS_BANNED))) {
            // Account reactivated
            notificationService.createAccountReactivatedNotification(user);
        }

        return savedUser;
    }

    private void cancelBookingsForOwnerProducts(User owner, String reason) {
        // Get all products owned by this user
        List<Product> ownerProducts = productRepository.findAll().stream()
            .filter(p -> p.getOwner() != null && p.getOwner().getId().equals(owner.getId()))
            .toList();

        for (Product product : ownerProducts) {
            // Get all active bookings for this product
            List<Booking> activeBookings = bookingRepository.findByProductId(product.getId()).stream()
                .filter(b -> Booking.STATUS_APPROVED.equals(b.getStatus()) || Booking.STATUS_PAID.equals(b.getStatus()))
                .toList();

            for (Booking booking : activeBookings) {
                booking.setStatus(Booking.STATUS_CANCELLED);
                bookingRepository.save(booking);
                // Notify the renter
                notificationService.createBookingCancelledByAdminNotification(booking.getRenter(), booking, reason);
            }
        }
    }

    private void cancelUserBookings(User renter, String reason) {
        List<Booking> activeBookings = bookingRepository.findByRenterId(renter.getId()).stream()
            .filter(b -> Booking.STATUS_APPROVED.equals(b.getStatus()) || Booking.STATUS_PAID.equals(b.getStatus()))
            .toList();

        for (Booking booking : activeBookings) {
            booking.setStatus(Booking.STATUS_CANCELLED);
            bookingRepository.save(booking);
            // Notify the product owner
            if (booking.getProduct().getOwner() != null) {
                notificationService.createBookingCancelledByAdminNotification(
                    booking.getProduct().getOwner(), booking, reason);
            }
        }
    }

    public User updateUserRole(Long userId, String role) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot modify admin user role");
        }

        String normalizedRole = role.toLowerCase().trim();
        if (normalizedRole.equals(User.ROLE_ADMIN)) {
            throw new IllegalArgumentException("Cannot set role to admin");
        }

        if (!normalizedRole.equals(User.ROLE_RENTER) && !normalizedRole.equals(User.ROLE_OWNER)) {
            throw new IllegalArgumentException("Invalid role. Only 'renter' or 'owner' are allowed");
        }

        user.setRole(normalizedRole);
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        if (User.ROLE_ADMIN.equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot delete admin user");
        }

        // Delete all notifications for this user
        List<Notification> userNotifications = notificationRepository.findByUserOrderByCreatedAtDesc(user);
        notificationRepository.deleteAll(userNotifications);

        // Delete bookings where user is renter (user_id is NOT NULL, can't set to null)
        List<Booking> userBookings = bookingRepository.findByRenterId(userId);
        for (Booking booking : userBookings) {
            // Delete notifications related to this booking
            List<Notification> bookingNotifications = notificationRepository.findByBooking(booking);
            notificationRepository.deleteAll(bookingNotifications);
            
            // Delete the booking
            bookingRepository.delete(booking);
        }

        // Handle products owned by this user
        List<Product> userProducts = productRepository.findAll().stream()
            .filter(p -> p.getOwner() != null && p.getOwner().getId().equals(userId))
            .toList();

        for (Product product : userProducts) {
            // Delete all bookings for this product
            List<Booking> productBookings = bookingRepository.findByProductId(product.getId());
            for (Booking booking : productBookings) {
                // Delete notifications related to this booking
                List<Notification> bookingNotifications = notificationRepository.findByBooking(booking);
                notificationRepository.deleteAll(bookingNotifications);
                
                // Delete the booking
                bookingRepository.delete(booking);
            }
            // Delete the product
            productRepository.delete(product);
        }

        // Now safe to delete the user
        userRepository.delete(user);
    }

    public AdminUserResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException(USER_NOT_FOUND_MSG));

        return mapUserToAdminResponse(user);
    }

    // Product management for admin
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        String productName = product.getName();
        User owner = product.getOwner();

        // Get all bookings for this product (active and historical)
        List<Booking> allBookings = bookingRepository.findByProductId(productId);

        // Notify renters and delete bookings
        for (Booking booking : allBookings) {
            // Notify the renter if booking was active
            if (Booking.STATUS_APPROVED.equals(booking.getStatus()) || Booking.STATUS_PAID.equals(booking.getStatus())) {
                notificationService.createProductDeletedNotification(booking.getRenter(), productName, false);
            }
            
            // Delete notifications related to this booking
            List<Notification> bookingNotifications = notificationRepository.findByBooking(booking);
            notificationRepository.deleteAll(bookingNotifications);
            
            // Delete the booking (product_id is NOT NULL, can't set to null)
            bookingRepository.delete(booking);
        }

        // Notify the owner
        if (owner != null) {
            notificationService.createProductDeletedNotification(owner, productName, true);
        }

        // Now safe to delete the product
        productRepository.delete(product);
    }
}
