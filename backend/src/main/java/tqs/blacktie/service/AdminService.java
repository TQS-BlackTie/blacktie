package tqs.blacktie.service;

import org.springframework.stereotype.Service;
import tqs.blacktie.dto.AdminUserResponse;
import tqs.blacktie.dto.PlatformMetricsResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public AdminService(UserRepository userRepository,
                       BookingRepository bookingRepository,
                       ProductRepository productRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    public boolean isAdmin(Long userId) {
        return userRepository.findById(userId)
            .map(user -> "admin".equals(user.getRole()))
            .orElse(false);
    }

    public PlatformMetricsResponse getPlatformMetrics() {
        long totalUsers = userRepository.count() - 1; // Exclude admin
        long totalOwners = userRepository.countByRole("owner");
        long totalRenters = userRepository.countByRole("renter");
        
        long totalProducts = productRepository.count();
        long availableProducts = productRepository.countByAvailableTrue();
        
        long totalBookings = bookingRepository.count();
        long activeBookings = bookingRepository.countByStatus(Booking.STATUS_ACTIVE);
        long completedBookings = bookingRepository.countByStatus(Booking.STATUS_COMPLETED);
        long cancelledBookings = bookingRepository.countByStatus(Booking.STATUS_CANCELLED);
        
        Double totalRevenueResult = bookingRepository.sumTotalPriceByStatus(Booking.STATUS_COMPLETED);
        double totalRevenue = totalRevenueResult != null ? totalRevenueResult : 0.0;
        double averageBookingValue = completedBookings > 0 ? totalRevenue / completedBookings : 0;
        
        return new PlatformMetricsResponse(
            totalUsers,
            totalOwners,
            totalRenters,
            totalProducts,
            availableProducts,
            totalBookings,
            activeBookings,
            completedBookings,
            cancelledBookings,
            totalRevenue,
            averageBookingValue
        );
    }

    public List<AdminUserResponse> getAllUsersForAdmin() {
        return userRepository.findByRoleNot("admin").stream()
            .map(user -> {
                long bookingsCount = bookingRepository.countByRenterId(user.getId());
                long productsCount = productRepository.countByOwnerId(user.getId());
                
                return new AdminUserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getRole(),
                    user.getStatus() != null ? user.getStatus() : "active",
                    user.getPhone(),
                    user.getAddress(),
                    user.getBusinessInfo(),
                    user.getCreatedAt().toString(),
                    bookingsCount,
                    productsCount
                );
            })
            .collect(Collectors.toList());
    }

    public User updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("admin".equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot modify admin user status");
        }

        String normalizedStatus = status.toLowerCase().trim();
        if (!normalizedStatus.equals("active") && 
            !normalizedStatus.equals("suspended") && 
            !normalizedStatus.equals("banned")) {
            throw new IllegalArgumentException("Invalid status. Only 'active', 'suspended', or 'banned' are allowed");
        }

        String previousStatus = user.getStatus();
        user.setStatus(normalizedStatus);
        User savedUser = userRepository.save(user);

        // Handle status change notifications and booking cancellations
        if (normalizedStatus.equals("suspended") || normalizedStatus.equals("banned")) {
            // Notify the user about their account status
            if (normalizedStatus.equals("suspended")) {
                notificationService.createAccountSuspendedNotification(user);
            } else {
                notificationService.createAccountBannedNotification(user);
            }

            // Cancel all active bookings for products owned by this user
            cancelBookingsForOwnerProducts(user, "Owner account " + normalizedStatus);

            // Cancel all active bookings made by this user as a renter
            cancelUserBookings(user, "Renter account " + normalizedStatus);
        } else if (normalizedStatus.equals("active") && 
                   (previousStatus != null && (previousStatus.equals("suspended") || previousStatus.equals("banned")))) {
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
                .filter(b -> Booking.STATUS_ACTIVE.equals(b.getStatus()))
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
            .filter(b -> Booking.STATUS_ACTIVE.equals(b.getStatus()))
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
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("admin".equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot modify admin user role");
        }

        String normalizedRole = role.toLowerCase().trim();
        if (normalizedRole.equals("admin")) {
            throw new IllegalArgumentException("Cannot set role to admin");
        }

        if (!normalizedRole.equals("renter") && !normalizedRole.equals("owner")) {
            throw new IllegalArgumentException("Invalid role. Only 'renter' or 'owner' are allowed");
        }

        user.setRole(normalizedRole);
        return userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if ("admin".equals(user.getRole())) {
            throw new IllegalArgumentException("Cannot delete admin user");
        }

        userRepository.delete(user);
    }

    public AdminUserResponse getUserDetails(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        long bookingsCount = bookingRepository.countByRenterId(user.getId());
        long productsCount = productRepository.countByOwnerId(user.getId());

        return new AdminUserResponse(
            user.getId(),
            user.getName(),
            user.getEmail(),
            user.getRole(),
            user.getStatus() != null ? user.getStatus() : "active",
            user.getPhone(),
            user.getAddress(),
            user.getBusinessInfo(),
            user.getCreatedAt().toString(),
            bookingsCount,
            productsCount
        );
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

        // Cancel all active bookings for this product and notify renters
        List<Booking> activeBookings = bookingRepository.findByProductId(productId).stream()
            .filter(b -> Booking.STATUS_ACTIVE.equals(b.getStatus()))
            .toList();

        for (Booking booking : activeBookings) {
            booking.setStatus(Booking.STATUS_CANCELLED);
            bookingRepository.save(booking);
            // Notify the renter
            notificationService.createProductDeletedNotification(booking.getRenter(), productName, false);
        }

        // Notify the owner
        if (owner != null) {
            notificationService.createProductDeletedNotification(owner, productName, true);
        }

        // Delete the product
        productRepository.delete(product);
    }
}
