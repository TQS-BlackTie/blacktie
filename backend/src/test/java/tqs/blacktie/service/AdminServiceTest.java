package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tqs.blacktie.dto.AdminUserResponse;
import tqs.blacktie.dto.PlatformMetricsResponse;
import tqs.blacktie.entity.Booking;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.BookingRepository;
import tqs.blacktie.repository.NotificationRepository;
import tqs.blacktie.repository.ProductRepository;
import tqs.blacktie.repository.UserRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminService Tests")
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User regularUser;
    private User ownerUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("Admin", "admin@test.com", "password", User.ROLE_ADMIN);
        adminUser.setId(1L);

        regularUser = new User("Regular User", "user@test.com", "password");
        regularUser.setId(2L);
        regularUser.setRole(User.ROLE_RENTER);

        ownerUser = new User("Owner User", "owner@test.com", "password");
        ownerUser.setId(3L);
        ownerUser.setRole(User.ROLE_OWNER);
    }

    @Nested
    @DisplayName("isAdmin Tests")
    class IsAdminTests {

        @Test
        @DisplayName("Should return true for admin user")
        void whenUserIsAdmin_thenReturnTrue() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertTrue(adminService.isAdmin(1L));
        }

        @Test
        @DisplayName("Should return false for non-admin user")
        void whenUserIsNotAdmin_thenReturnFalse() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

            assertFalse(adminService.isAdmin(2L));
        }

        @Test
        @DisplayName("Should return false for non-existent user")
        void whenUserDoesNotExist_thenReturnFalse() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertFalse(adminService.isAdmin(999L));
        }
    }

    @Nested
    @DisplayName("getPlatformMetrics Tests")
    class GetPlatformMetricsTests {

        @Test
        @DisplayName("Should return correct platform metrics")
        void whenGetMetrics_thenReturnCorrectValues() {
            when(userRepository.count()).thenReturn(11L); // 10 users + 1 admin
            when(userRepository.countByRole(User.ROLE_OWNER)).thenReturn(3L);
            when(userRepository.countByRole(User.ROLE_RENTER)).thenReturn(7L);
            when(productRepository.count()).thenReturn(20L);
            when(productRepository.countByAvailableTrue()).thenReturn(15L);
            when(bookingRepository.count()).thenReturn(50L);
            when(bookingRepository.countByStatus(Booking.STATUS_PAID)).thenReturn(10L);
            when(bookingRepository.countByStatus(Booking.STATUS_COMPLETED)).thenReturn(35L);
            when(bookingRepository.countByStatus(Booking.STATUS_CANCELLED)).thenReturn(5L);
            when(bookingRepository.sumTotalPriceByStatus(Booking.STATUS_COMPLETED)).thenReturn(3500.0);

            PlatformMetricsResponse metrics = adminService.getPlatformMetrics();

            assertEquals(10L, metrics.getTotalUsers());
            assertEquals(3L, metrics.getTotalOwners());
            assertEquals(7L, metrics.getTotalRenters());
            assertEquals(20L, metrics.getTotalProducts());
            assertEquals(15L, metrics.getAvailableProducts());
            assertEquals(50L, metrics.getTotalBookings());
            assertEquals(10L, metrics.getActiveBookings());
            assertEquals(35L, metrics.getCompletedBookings());
            assertEquals(5L, metrics.getCancelledBookings());
            assertEquals(3500.0, metrics.getTotalRevenue());
            assertEquals(100.0, metrics.getAverageBookingValue());
        }

        @Test
        @DisplayName("Should handle null revenue gracefully")
        void whenRevenueIsNull_thenReturnZero() {
            when(userRepository.count()).thenReturn(1L);
            when(userRepository.countByRole(any())).thenReturn(0L);
            when(productRepository.count()).thenReturn(0L);
            when(productRepository.countByAvailableTrue()).thenReturn(0L);
            when(bookingRepository.count()).thenReturn(0L);
            when(bookingRepository.countByStatus(any())).thenReturn(0L);
            when(bookingRepository.sumTotalPriceByStatus(Booking.STATUS_COMPLETED)).thenReturn(null);

            PlatformMetricsResponse metrics = adminService.getPlatformMetrics();

            assertEquals(0.0, metrics.getTotalRevenue());
            assertEquals(0.0, metrics.getAverageBookingValue());
        }
    }

    @Nested
    @DisplayName("getAllUsersForAdmin Tests")
    class GetAllUsersForAdminTests {

        @Test
        @DisplayName("Should return all non-admin users")
        void whenGetAllUsers_thenReturnNonAdminUsers() {
            when(userRepository.findByRoleNot(User.ROLE_ADMIN)).thenReturn(Arrays.asList(regularUser, ownerUser));
            when(bookingRepository.countByRenterId(any())).thenReturn(5L);
            when(productRepository.countByOwnerId(any())).thenReturn(2L);

            List<AdminUserResponse> users = adminService.getAllUsersForAdmin();

            assertEquals(2, users.size());
            verify(userRepository).findByRoleNot(User.ROLE_ADMIN);
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void whenNoUsers_thenReturnEmptyList() {
            when(userRepository.findByRoleNot(User.ROLE_ADMIN)).thenReturn(Collections.emptyList());

            List<AdminUserResponse> users = adminService.getAllUsersForAdmin();

            assertTrue(users.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateUserStatus Tests")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should update user status to suspended")
        void whenUpdateToSuspended_thenStatusChanged() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(bookingRepository.findByRenterId(any())).thenReturn(Collections.emptyList());
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            User result = adminService.updateUserStatus(2L, "suspended");

            assertEquals(User.STATUS_SUSPENDED, result.getStatus());
            verify(notificationService).createAccountSuspendedNotification(regularUser);
        }

        @Test
        @DisplayName("Should update user status to banned")
        void whenUpdateToBanned_thenStatusChanged() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(bookingRepository.findByRenterId(any())).thenReturn(Collections.emptyList());
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            User result = adminService.updateUserStatus(2L, "banned");

            assertEquals(User.STATUS_BANNED, result.getStatus());
            verify(notificationService).createAccountBannedNotification(regularUser);
        }

        @Test
        @DisplayName("Should reactivate user and send notification")
        void whenReactivateUser_thenNotificationSent() {
            regularUser.setStatus(User.STATUS_SUSPENDED);
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);

            adminService.updateUserStatus(2L, "active");

            verify(notificationService).createAccountReactivatedNotification(regularUser);
        }

        @Test
        @DisplayName("Should reactivate banned user and send notification")
        void whenReactivateBannedUser_thenNotificationSent() {
            regularUser.setStatus(User.STATUS_BANNED);
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);

            adminService.updateUserStatus(2L, "active");

            verify(notificationService).createAccountReactivatedNotification(regularUser);
        }

        @Test
        @DisplayName("Should cancel owner products bookings when suspending")
        void whenSuspendOwner_thenCancelProductBookings() {
            Product product = new Product();
            product.setId(1L);
            product.setOwner(ownerUser);

            Booking activeBooking = new Booking();
            activeBooking.setId(1L);
            activeBooking.setStatus(Booking.STATUS_APPROVED);
            activeBooking.setRenter(regularUser);

            when(userRepository.findById(3L)).thenReturn(Optional.of(ownerUser));
            when(userRepository.save(any(User.class))).thenReturn(ownerUser);
            when(productRepository.findAll()).thenReturn(List.of(product));
            when(bookingRepository.findByProductId(1L)).thenReturn(List.of(activeBooking));
            when(bookingRepository.findByRenterId(3L)).thenReturn(Collections.emptyList());

            adminService.updateUserStatus(3L, "suspended");

            verify(bookingRepository).save(activeBooking);
            assertEquals(Booking.STATUS_CANCELLED, activeBooking.getStatus());
            verify(notificationService).createBookingCancelledByAdminNotification(eq(regularUser), eq(activeBooking), anyString());
        }

        @Test
        @DisplayName("Should cancel user bookings when suspending renter")
        void whenSuspendRenter_thenCancelUserBookings() {
            Product product = new Product();
            product.setId(1L);
            product.setOwner(ownerUser);

            Booking activeBooking = new Booking();
            activeBooking.setId(1L);
            activeBooking.setStatus(Booking.STATUS_APPROVED);
            activeBooking.setRenter(regularUser);
            activeBooking.setProduct(product);

            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(productRepository.findAll()).thenReturn(Collections.emptyList());
            when(bookingRepository.findByRenterId(2L)).thenReturn(List.of(activeBooking));

            adminService.updateUserStatus(2L, "banned");

            verify(bookingRepository).save(activeBooking);
            assertEquals(Booking.STATUS_CANCELLED, activeBooking.getStatus());
            verify(notificationService).createBookingCancelledByAdminNotification(eq(ownerUser), eq(activeBooking), anyString());
        }

        @Test
        @DisplayName("Should not send reactivation notification when status is same")
        void whenActivateAlreadyActiveUser_thenNoReactivationNotification() {
            regularUser.setStatus(User.STATUS_ACTIVE);
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);

            adminService.updateUserStatus(2L, "active");

            verify(notificationService, never()).createAccountReactivatedNotification(any());
        }

        @Test
        @DisplayName("Should skip products without owner when cancelling owner bookings")
        void whenSuspendUser_thenSkipProductsWithoutOwner() {
            Product productWithoutOwner = new Product();
            productWithoutOwner.setId(1L);
            productWithoutOwner.setOwner(null);

            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(productRepository.findAll()).thenReturn(List.of(productWithoutOwner));
            when(bookingRepository.findByRenterId(2L)).thenReturn(Collections.emptyList());

            adminService.updateUserStatus(2L, "suspended");

            verify(bookingRepository, never()).findByProductId(any());
        }

        @Test
        @DisplayName("Should skip completed bookings when cancelling")
        void whenSuspendUser_thenSkipCompletedBookings() {
            Product product = new Product();
            product.setId(1L);
            product.setOwner(ownerUser);

            Booking completedBooking = new Booking();
            completedBooking.setId(1L);
            completedBooking.setStatus(Booking.STATUS_COMPLETED);
            completedBooking.setRenter(regularUser);

            when(userRepository.findById(3L)).thenReturn(Optional.of(ownerUser));
            when(userRepository.save(any(User.class))).thenReturn(ownerUser);
            when(productRepository.findAll()).thenReturn(List.of(product));
            when(bookingRepository.findByProductId(1L)).thenReturn(List.of(completedBooking));
            when(bookingRepository.findByRenterId(3L)).thenReturn(Collections.emptyList());

            adminService.updateUserStatus(3L, "suspended");

            // Should not save completed booking
            verify(bookingRepository, never()).save(completedBooking);
        }

        @Test
        @DisplayName("Should handle booking without product owner when cancelling renter bookings")
        void whenCancelRenterBooking_thenSkipNotificationIfNoOwner() {
            Product productWithoutOwner = new Product();
            productWithoutOwner.setId(1L);
            productWithoutOwner.setOwner(null);

            Booking activeBooking = new Booking();
            activeBooking.setId(1L);
            activeBooking.setStatus(Booking.STATUS_APPROVED);
            activeBooking.setRenter(regularUser);
            activeBooking.setProduct(productWithoutOwner);

            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);
            when(productRepository.findAll()).thenReturn(Collections.emptyList());
            when(bookingRepository.findByRenterId(2L)).thenReturn(List.of(activeBooking));

            adminService.updateUserStatus(2L, "banned");

            verify(bookingRepository).save(activeBooking);
            // Should not notify owner since owner is null
            verify(notificationService, never()).createBookingCancelledByAdminNotification(eq(null), any(), anyString());
        }

        @Test
        @DisplayName("Should throw exception for admin user")
        void whenUpdateAdminStatus_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserStatus(1L, "suspended"));
        }

        @Test
        @DisplayName("Should throw exception for invalid status")
        void whenInvalidStatus_thenThrowException() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserStatus(2L, "invalid"));
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void whenUserNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserStatus(999L, "suspended"));
        }
    }

    @Nested
    @DisplayName("updateUserRole Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should update user role to owner")
        void whenUpdateToOwner_thenRoleChanged() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(userRepository.save(any(User.class))).thenReturn(regularUser);

            User result = adminService.updateUserRole(2L, "owner");

            assertEquals(User.ROLE_OWNER, result.getRole());
        }

        @Test
        @DisplayName("Should throw exception when trying to set admin role")
        void whenSetAdminRole_thenThrowException() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserRole(2L, "admin"));
        }

        @Test
        @DisplayName("Should throw exception for admin user")
        void whenUpdateAdminRole_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserRole(1L, "owner"));
        }

        @Test
        @DisplayName("Should throw exception for invalid role")
        void whenInvalidRole_thenThrowException() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.updateUserRole(2L, "invalid"));
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete regular user")
        void whenDeleteRegularUser_thenUserDeleted() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(notificationRepository.findByUserOrderByCreatedAtDesc(regularUser)).thenReturn(Collections.emptyList());
            when(bookingRepository.findByRenterId(2L)).thenReturn(Collections.emptyList());
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

            adminService.deleteUser(2L);

            verify(userRepository).delete(regularUser);
        }

        @Test
        @DisplayName("Should throw exception when deleting admin")
        void whenDeleteAdmin_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.deleteUser(1L));
            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void whenUserNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.deleteUser(999L));
        }
    }

    @Nested
    @DisplayName("getUserDetails Tests")
    class GetUserDetailsTests {

        @Test
        @DisplayName("Should return user details")
        void whenGetUserDetails_thenReturnDetails() {
            when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
            when(bookingRepository.countByRenterId(2L)).thenReturn(5L);
            when(productRepository.countByOwnerId(2L)).thenReturn(0L);

            AdminUserResponse response = adminService.getUserDetails(2L);

            assertEquals(2L, response.getId());
            assertEquals("Regular User", response.getName());
            assertEquals(5L, response.getBookingsCount());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void whenUserNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.getUserDetails(999L));
        }
    }

    @Nested
    @DisplayName("Product Management Tests")
    class ProductManagementTests {

        @Test
        @DisplayName("Should return all products")
        void whenGetAllProducts_thenReturnProducts() {
            Product product1 = new Product();
            product1.setId(1L);
            Product product2 = new Product();
            product2.setId(2L);

            when(productRepository.findAll()).thenReturn(Arrays.asList(product1, product2));

            List<Product> products = adminService.getAllProducts();

            assertEquals(2, products.size());
        }

        @Test
        @DisplayName("Should delete product and notify affected users")
        void whenDeleteProduct_thenNotifyUsers() {
            Product product = new Product();
            product.setId(1L);
            product.setName("Test Product");
            product.setOwner(ownerUser);

            Booking activeBooking = new Booking();
            activeBooking.setId(1L);
            activeBooking.setStatus(Booking.STATUS_APPROVED);
            activeBooking.setRenter(regularUser);
            activeBooking.setProduct(product);

            when(productRepository.findById(1L)).thenReturn(Optional.of(product));
            when(bookingRepository.findByProductId(1L)).thenReturn(List.of(activeBooking));
            when(notificationRepository.findByBooking(activeBooking)).thenReturn(Collections.emptyList());

            adminService.deleteProduct(1L);

            verify(notificationService).createProductDeletedNotification(regularUser, "Test Product", false);
            verify(notificationService).createProductDeletedNotification(ownerUser, "Test Product", true);
            verify(bookingRepository).delete(activeBooking);
            verify(productRepository).delete(product);
        }

        @Test
        @DisplayName("Should throw exception for non-existent product")
        void whenProductNotFound_thenThrowException() {
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class, () -> 
                adminService.deleteProduct(999L));
        }
    }
}
