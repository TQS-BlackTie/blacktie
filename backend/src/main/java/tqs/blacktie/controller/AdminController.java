package tqs.blacktie.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tqs.blacktie.dto.*;
import tqs.blacktie.entity.Product;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.AdminService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final String MESSAGE_KEY = "message";

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Middleware to check if user is admin
     */
    private ResponseEntity<Map<String, String>> checkAdminAccess(Long userId) {
        if (userId == null) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "User ID is required");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
        
        if (!adminService.isAdmin(userId)) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "Access denied. Admin privileges required.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
        }
        
        return null;
    }

    @GetMapping("/metrics")
    public ResponseEntity<Object> getPlatformMetrics(@RequestHeader("X-User-Id") Long userId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            PlatformMetricsResponse metrics = adminService.getPlatformMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "Failed to fetch metrics: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/users")
    public ResponseEntity<Object> getAllUsers(@RequestHeader("X-User-Id") Long userId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            List<AdminUserResponse> users = adminService.getAllUsersForAdmin();
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "Failed to fetch users: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/users/{targetUserId}")
    public ResponseEntity<Object> getUserDetails(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            AdminUserResponse user = adminService.getUserDetails(targetUserId);
            return ResponseEntity.ok(user);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @PutMapping("/users/{targetUserId}/status")
    public ResponseEntity<Object> updateUserStatus(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            User updatedUser = adminService.updateUserStatus(targetUserId, request.getStatus());
            AdminUserResponse response = adminService.getUserDetails(updatedUser.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @PutMapping("/users/{targetUserId}/role")
    public ResponseEntity<Object> updateUserRole(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody SetRoleRequest request) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            User updatedUser = adminService.updateUserRole(targetUserId, request.getRole());
            AdminUserResponse response = adminService.getUserDetails(updatedUser.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    @DeleteMapping("/users/{targetUserId}")
    public ResponseEntity<Object> deleteUser(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            adminService.deleteUser(targetUserId);
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "User deleted successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }

    // ==================== Product Management ====================

    @GetMapping("/products")
    public ResponseEntity<Object> getAllProducts(@RequestHeader("X-User-Id") Long userId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            List<Product> products = adminService.getAllProducts();
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "Failed to fetch products: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @DeleteMapping("/products/{productId}")
    public ResponseEntity<Object> deleteProduct(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long productId) {
        ResponseEntity<Map<String, String>> accessCheck = checkAdminAccess(userId);
        if (accessCheck != null) return ResponseEntity.status(accessCheck.getStatusCode()).body(accessCheck.getBody());

        try {
            adminService.deleteProduct(productId);
            Map<String, String> response = new HashMap<>();
            response.put(MESSAGE_KEY, "Product deleted successfully. Affected users have been notified.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put(MESSAGE_KEY, "Failed to delete product: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
}
