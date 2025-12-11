package tqs.blacktie.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DTO Tests")
class DtoTest {

    @Nested
    @DisplayName("LoginRequest Tests")
    class LoginRequestTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            LoginRequest request = new LoginRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            LoginRequest request = new LoginRequest("john@example.com", "password123");

            assertEquals("john@example.com", request.getEmail());
            assertEquals("password123", request.getPassword());
        }

        @Test
        @DisplayName("Should get and set email")
        void whenSetEmail_thenGetEmail() {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");

            assertEquals("test@example.com", request.getEmail());
        }

        @Test
        @DisplayName("Should get and set password")
        void whenSetPassword_thenGetPassword() {
            LoginRequest request = new LoginRequest();
            request.setPassword("newPassword");

            assertEquals("newPassword", request.getPassword());
        }
    }

    @Nested
    @DisplayName("LoginResponse Tests")
    class LoginResponseTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            LoginResponse response = new LoginResponse();
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            LoginResponse response = new LoginResponse(1L, "John", "john@example.com", "renter", "Login successful");

            assertEquals(1L, response.getId());
            assertEquals("John", response.getName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("renter", response.getRole());
            assertEquals("Login successful", response.getMessage());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            LoginResponse response = new LoginResponse();
            response.setId(2L);
            response.setName("Jane");
            response.setEmail("jane@example.com");
            response.setRole("owner");
            response.setMessage("Success");

            assertEquals(2L, response.getId());
            assertEquals("Jane", response.getName());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("owner", response.getRole());
            assertEquals("Success", response.getMessage());
        }
    }

    @Nested
    @DisplayName("SignUpRequest Tests")
    class SignUpRequestTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            SignUpRequest request = new SignUpRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "password123");

            assertEquals("John Doe", request.getName());
            assertEquals("john@example.com", request.getEmail());
            assertEquals("password123", request.getPassword());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            SignUpRequest request = new SignUpRequest();
            request.setName("Jane Doe");
            request.setEmail("jane@example.com");
            request.setPassword("securePass");

            assertEquals("Jane Doe", request.getName());
            assertEquals("jane@example.com", request.getEmail());
            assertEquals("securePass", request.getPassword());
        }
    }

    @Nested
    @DisplayName("SignUpResponse Tests")
    class SignUpResponseTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            SignUpResponse response = new SignUpResponse();
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            SignUpResponse response = new SignUpResponse(1L, "John", "john@example.com", "Account created");

            assertEquals(1L, response.getId());
            assertEquals("John", response.getName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("Account created", response.getMessage());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            SignUpResponse response = new SignUpResponse();
            response.setId(2L);
            response.setName("Jane");
            response.setEmail("jane@example.com");
            response.setMessage("Success");

            assertEquals(2L, response.getId());
            assertEquals("Jane", response.getName());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("Success", response.getMessage());
        }
    }

    @Nested
    @DisplayName("SetRoleRequest Tests")
    class SetRoleRequestTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            SetRoleRequest request = new SetRoleRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            SetRoleRequest request = new SetRoleRequest("owner");

            assertEquals("owner", request.getRole());
        }

        @Test
        @DisplayName("Should get and set role")
        void whenSetRole_thenGetRole() {
            SetRoleRequest request = new SetRoleRequest();
            request.setRole("renter");

            assertEquals("renter", request.getRole());
        }
    }

    @Nested
    @DisplayName("UpdateProfileRequest Tests")
    class UpdateProfileRequestTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "John Doe", "+351912345678", "123 Main St", "Premium service"
            );

            assertEquals("John Doe", request.getName());
            assertEquals("+351912345678", request.getPhone());
            assertEquals("123 Main St", request.getAddress());
            assertEquals("Premium service", request.getBusinessInfo());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            UpdateProfileRequest request = new UpdateProfileRequest();
            request.setName("Jane Doe");
            request.setPhone("+351987654321");
            request.setAddress("456 New Ave");
            request.setBusinessInfo("Business info");

            assertEquals("Jane Doe", request.getName());
            assertEquals("+351987654321", request.getPhone());
            assertEquals("456 New Ave", request.getAddress());
            assertEquals("Business info", request.getBusinessInfo());
        }
    }

    @Nested
    @DisplayName("UserResponse Tests")
    class UserResponseTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            UserResponse response = new UserResponse();
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should create with 5-args constructor")
        void whenFiveArgsConstructor_thenCreate() {
            UserResponse response = new UserResponse(1L, "John", "john@example.com", "renter", "2024-01-01T10:00:00");

            assertEquals(1L, response.getId());
            assertEquals("John", response.getName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("renter", response.getRole());
            assertEquals("2024-01-01T10:00:00", response.getCreatedAt());
        }

        @Test
        @DisplayName("Should create with 8-args constructor")
        void whenEightArgsConstructor_thenCreate() {
            UserResponse response = new UserResponse(
                    1L, "John", "john@example.com", "owner",
                    "+351912345678", "123 Main St", "Business info", "2024-01-01T10:00:00"
            );

            assertEquals(1L, response.getId());
            assertEquals("John", response.getName());
            assertEquals("john@example.com", response.getEmail());
            assertEquals("owner", response.getRole());
            assertEquals("+351912345678", response.getPhone());
            assertEquals("123 Main St", response.getAddress());
            assertEquals("Business info", response.getBusinessInfo());
            assertEquals("2024-01-01T10:00:00", response.getCreatedAt());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            UserResponse response = new UserResponse();
            response.setId(2L);
            response.setName("Jane");
            response.setEmail("jane@example.com");
            response.setRole("admin");
            response.setPhone("+351111111111");
            response.setAddress("New Address");
            response.setBusinessInfo("New Business");
            response.setCreatedAt("2024-12-01T12:00:00");

            assertEquals(2L, response.getId());
            assertEquals("Jane", response.getName());
            assertEquals("jane@example.com", response.getEmail());
            assertEquals("admin", response.getRole());
            assertEquals("+351111111111", response.getPhone());
            assertEquals("New Address", response.getAddress());
            assertEquals("New Business", response.getBusinessInfo());
            assertEquals("2024-12-01T12:00:00", response.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("PaymentIntentRequest Tests")
    class PaymentIntentRequestTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            PaymentIntentRequest request = new PaymentIntentRequest();
            assertNotNull(request);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            PaymentIntentRequest request = new PaymentIntentRequest(10L, 5000L);

            assertEquals(10L, request.getBookingId());
            assertEquals(5000L, request.getAmount());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            PaymentIntentRequest request = new PaymentIntentRequest();
            request.setBookingId(20L);
            request.setAmount(7500L);

            assertEquals(20L, request.getBookingId());
            assertEquals(7500L, request.getAmount());
        }
    }

    @Nested
    @DisplayName("PaymentIntentResponse Tests")
    class PaymentIntentResponseTests {

        @Test
        @DisplayName("Should create with no-args constructor")
        void whenNoArgsConstructor_thenCreate() {
            PaymentIntentResponse response = new PaymentIntentResponse();
            assertNotNull(response);
        }

        @Test
        @DisplayName("Should create with all-args constructor")
        void whenAllArgsConstructor_thenCreate() {
            PaymentIntentResponse response = new PaymentIntentResponse("secret", "pi_123", 5000L, "eur");

            assertEquals("secret", response.getClientSecret());
            assertEquals("pi_123", response.getPaymentIntentId());
            assertEquals(5000L, response.getAmount());
            assertEquals("eur", response.getCurrency());
        }

        @Test
        @DisplayName("Should get and set all fields")
        void whenSetFields_thenGetFields() {
            PaymentIntentResponse response = new PaymentIntentResponse();
            response.setClientSecret("another_secret");
            response.setPaymentIntentId("pi_456");
            response.setAmount(12000L);
            response.setCurrency("usd");

            assertEquals("another_secret", response.getClientSecret());
            assertEquals("pi_456", response.getPaymentIntentId());
            assertEquals(12000L, response.getAmount());
            assertEquals("usd", response.getCurrency());
        }
    }
}
