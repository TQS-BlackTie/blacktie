package tqs.blacktie.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Admin DTO Tests")
class AdminDtoTest {

    @Test
    @DisplayName("PlatformMetricsResponse getters and setters")
    void platformMetricsResponse_GettersAndSetters() {
        PlatformMetricsResponse response = new PlatformMetricsResponse();
        
        response.setTotalUsers(100);
        response.setTotalOwners(30);
        response.setTotalRenters(70);
        response.setTotalProducts(50);
        response.setAvailableProducts(40);
        response.setTotalBookings(200);
        response.setActiveBookings(20);
        response.setCompletedBookings(170);
        response.setCancelledBookings(10);
        response.setTotalRevenue(5000.0);
        response.setAverageBookingValue(25.0);

        assertEquals(100, response.getTotalUsers());
        assertEquals(30, response.getTotalOwners());
        assertEquals(70, response.getTotalRenters());
        assertEquals(50, response.getTotalProducts());
        assertEquals(40, response.getAvailableProducts());
        assertEquals(200, response.getTotalBookings());
        assertEquals(20, response.getActiveBookings());
        assertEquals(170, response.getCompletedBookings());
        assertEquals(10, response.getCancelledBookings());
        assertEquals(5000.0, response.getTotalRevenue());
        assertEquals(25.0, response.getAverageBookingValue());
    }

    @Test
    @DisplayName("AdminUserResponse getters and setters")
    void adminUserResponse_GettersAndSetters() {
        AdminUserResponse response = new AdminUserResponse();
        
        response.setId(1L);
        response.setName("Test User");
        response.setEmail("test@test.com");
        response.setRole("renter");
        response.setStatus("active");
        response.setPhone("123456789");
        response.setAddress("Test Address");
        response.setBusinessInfo("Test Business");
        response.setCreatedAt("2024-01-01");
        response.setBookingsCount(10);
        response.setProductsCount(5);

        assertEquals(1L, response.getId());
        assertEquals("Test User", response.getName());
        assertEquals("test@test.com", response.getEmail());
        assertEquals("renter", response.getRole());
        assertEquals("active", response.getStatus());
        assertEquals("123456789", response.getPhone());
        assertEquals("Test Address", response.getAddress());
        assertEquals("Test Business", response.getBusinessInfo());
        assertEquals("2024-01-01", response.getCreatedAt());
        assertEquals(10, response.getBookingsCount());
        assertEquals(5, response.getProductsCount());
    }

    @Test
    @DisplayName("UpdateUserStatusRequest getters and setters")
    void updateUserStatusRequest_GettersAndSetters() {
        UpdateUserStatusRequest request = new UpdateUserStatusRequest();
        
        request.setStatus("suspended");

        assertEquals("suspended", request.getStatus());
    }

    @Test
    @DisplayName("SetRoleRequest getters and setters")
    void setRoleRequest_GettersAndSetters() {
        SetRoleRequest request = new SetRoleRequest();
        
        request.setRole("owner");

        assertEquals("owner", request.getRole());
    }
}
