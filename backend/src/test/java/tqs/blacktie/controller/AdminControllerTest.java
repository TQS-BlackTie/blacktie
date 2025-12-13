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
import tqs.blacktie.dto.AdminUserResponse;
import tqs.blacktie.dto.PlatformMetricsResponse;
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.UpdateUserStatusRequest;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.AdminService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @InjectMocks
    private AdminController adminController;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new User("Admin", "admin@test.com", "password", User.ROLE_ADMIN);
        adminUser.setId(1L);
    }

    @Nested
    @DisplayName("Access Control Tests")
    class AccessControlTests {

        @Test
        @DisplayName("Should return unauthorized when userId is null")
        void whenUserIdIsNull_thenReturnUnauthorized() {
            ResponseEntity<Object> response = adminController.getPlatformMetrics(null);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertNotNull(body);
            assertEquals("User ID is required", body.get("message"));
        }

        @Test
        @DisplayName("Should return forbidden when user is not admin")
        void whenUserIsNotAdmin_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.getPlatformMetrics(2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertNotNull(body);
            assertEquals("Access denied. Admin privileges required.", body.get("message"));
        }
    }

    @Nested
    @DisplayName("getPlatformMetrics Tests")
    class GetPlatformMetricsTests {

        @Test
        @DisplayName("Should return metrics for admin user")
        void whenAdminUser_thenReturnMetrics() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            PlatformMetricsResponse metrics = new PlatformMetricsResponse();
            metrics.setTotalUsers(10);
            when(adminService.getPlatformMetrics()).thenReturn(metrics);

            ResponseEntity<Object> response = adminController.getPlatformMetrics(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertNotNull(response.getBody());
        }

        @Test
        @DisplayName("Should return error on exception")
        void whenException_thenReturnError() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.getPlatformMetrics()).thenThrow(new RuntimeException("Database error"));

            ResponseEntity<Object> response = adminController.getPlatformMetrics(1L);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("getAllUsers Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users for admin")
        void whenAdminUser_thenReturnUsers() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            AdminUserResponse user1 = new AdminUserResponse();
            user1.setId(2L);
            AdminUserResponse user2 = new AdminUserResponse();
            user2.setId(3L);
            when(adminService.getAllUsersForAdmin()).thenReturn(Arrays.asList(user1, user2));

            ResponseEntity<Object> response = adminController.getAllUsers(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            List<AdminUserResponse> users = (List<AdminUserResponse>) response.getBody();
            assertNotNull(users);
            assertEquals(2, users.size());
        }
    }

    @Nested
    @DisplayName("getUserDetails Tests")
    class GetUserDetailsTests {

        @Test
        @DisplayName("Should return user details")
        void whenAdminUser_thenReturnUserDetails() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            AdminUserResponse userResponse = new AdminUserResponse();
            userResponse.setId(2L);
            userResponse.setName("Test User");
            when(adminService.getUserDetails(2L)).thenReturn(userResponse);

            ResponseEntity<Object> response = adminController.getUserDetails(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return not found for non-existent user")
        void whenUserNotFound_thenReturnNotFound() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.getUserDetails(999L)).thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<Object> response = adminController.getUserDetails(1L, 999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("updateUserStatus Tests")
    class UpdateUserStatusTests {

        @Test
        @DisplayName("Should update user status")
        void whenValidRequest_thenUpdateStatus() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            User updatedUser = new User("Test", "test@test.com", "pass");
            updatedUser.setId(2L);
            updatedUser.setStatus(User.STATUS_SUSPENDED);
            when(adminService.updateUserStatus(2L, "suspended")).thenReturn(updatedUser);
            AdminUserResponse userResponse = new AdminUserResponse();
            userResponse.setId(2L);
            when(adminService.getUserDetails(2L)).thenReturn(userResponse);

            UpdateUserStatusRequest request = new UpdateUserStatusRequest();
            request.setStatus("suspended");

            ResponseEntity<Object> response = adminController.updateUserStatus(1L, 2L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request for invalid status")
        void whenInvalidStatus_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.updateUserStatus(eq(2L), any())).thenThrow(new IllegalArgumentException("Invalid status"));

            UpdateUserStatusRequest request = new UpdateUserStatusRequest();
            request.setStatus("invalid");

            ResponseEntity<Object> response = adminController.updateUserStatus(1L, 2L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("updateUserRole Tests")
    class UpdateUserRoleTests {

        @Test
        @DisplayName("Should update user role")
        void whenValidRequest_thenUpdateRole() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            User updatedUser = new User("Test", "test@test.com", "pass");
            updatedUser.setId(2L);
            updatedUser.setRole(User.ROLE_OWNER);
            when(adminService.updateUserRole(2L, "owner")).thenReturn(updatedUser);
            AdminUserResponse userResponse = new AdminUserResponse();
            userResponse.setId(2L);
            when(adminService.getUserDetails(2L)).thenReturn(userResponse);

            SetRoleRequest request = new SetRoleRequest();
            request.setRole("owner");

            ResponseEntity<Object> response = adminController.updateUserRole(1L, 2L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("deleteUser Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user successfully")
        void whenValidRequest_thenDeleteUser() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            doNothing().when(adminService).deleteUser(2L);

            ResponseEntity<Object> response = adminController.deleteUser(1L, 2L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertNotNull(body);
            assertEquals("User deleted successfully", body.get("message"));
        }

        @Test
        @DisplayName("Should return bad request when deleting admin")
        void whenDeletingAdmin_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            doThrow(new IllegalArgumentException("Cannot delete admin")).when(adminService).deleteUser(1L);

            ResponseEntity<Object> response = adminController.deleteUser(1L, 1L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Product Management Tests")
    class ProductManagementTests {

        @Test
        @DisplayName("Should return all products")
        void whenAdminUser_thenReturnProducts() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            Product product1 = new Product();
            product1.setId(1L);
            Product product2 = new Product();
            product2.setId(2L);
            when(adminService.getAllProducts()).thenReturn(Arrays.asList(product1, product2));

            ResponseEntity<Object> response = adminController.getAllProducts(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            List<Product> products = (List<Product>) response.getBody();
            assertNotNull(products);
            assertEquals(2, products.size());
        }

        @Test
        @DisplayName("Should delete product successfully")
        void whenValidRequest_thenDeleteProduct() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            doNothing().when(adminService).deleteProduct(1L);

            ResponseEntity<Object> response = adminController.deleteProduct(1L, 1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            @SuppressWarnings("unchecked")
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertNotNull(body);
            assertTrue(body.get("message").contains("deleted successfully"));
        }

        @Test
        @DisplayName("Should return not found for non-existent product")
        void whenProductNotFound_thenReturnNotFound() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            doThrow(new IllegalArgumentException("Product not found")).when(adminService).deleteProduct(999L);

            ResponseEntity<Object> response = adminController.deleteProduct(1L, 999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to get products")
        void whenNonAdminUser_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.getAllProducts(2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to delete product")
        void whenNonAdminDeleteProduct_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.deleteProduct(2L, 1L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }
    }

    @Nested
    @DisplayName("Additional Edge Cases Tests")
    class AdditionalEdgeCasesTests {

        @Test
        @DisplayName("Should return forbidden when non-admin tries to get metrics")
        void whenNonAdminGetMetrics_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.getPlatformMetrics(2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to get users")
        void whenNonAdminGetUsers_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.getAllUsers(2L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to update status")
        void whenNonAdminUpdateStatus_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            UpdateUserStatusRequest request = new UpdateUserStatusRequest();
            request.setStatus("suspended");

            ResponseEntity<Object> response = adminController.updateUserStatus(2L, 3L, request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to update role")
        void whenNonAdminUpdateRole_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            SetRoleRequest request = new SetRoleRequest();
            request.setRole("owner");

            ResponseEntity<Object> response = adminController.updateUserRole(2L, 3L, request);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return forbidden when non-admin tries to delete user")
        void whenNonAdminDeleteUser_thenReturnForbidden() {
            when(adminService.isAdmin(2L)).thenReturn(false);

            ResponseEntity<Object> response = adminController.deleteUser(2L, 3L);

            assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request for invalid role")
        void whenInvalidRole_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.updateUserRole(2L, "invalid")).thenThrow(new IllegalArgumentException("Invalid role"));

            SetRoleRequest request = new SetRoleRequest();
            request.setRole("invalid");

            ResponseEntity<Object> response = adminController.updateUserRole(1L, 2L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request for user status update when user not found")
        void whenUpdateStatusUserNotFound_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.updateUserStatus(999L, "suspended")).thenThrow(new IllegalArgumentException("User not found"));

            UpdateUserStatusRequest request = new UpdateUserStatusRequest();
            request.setStatus("suspended");

            ResponseEntity<Object> response = adminController.updateUserStatus(1L, 999L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request for user role update when user not found")
        void whenUpdateRoleUserNotFound_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            when(adminService.updateUserRole(999L, "owner")).thenThrow(new IllegalArgumentException("User not found"));

            SetRoleRequest request = new SetRoleRequest();
            request.setRole("owner");

            ResponseEntity<Object> response = adminController.updateUserRole(1L, 999L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }

        @Test
        @DisplayName("Should return bad request when deleting non-existent user")
        void whenDeleteNonExistentUser_thenReturnBadRequest() {
            when(adminService.isAdmin(1L)).thenReturn(true);
            doThrow(new IllegalArgumentException("User not found")).when(adminService).deleteUser(999L);

            ResponseEntity<Object> response = adminController.deleteUser(1L, 999L);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        }
    }
}
