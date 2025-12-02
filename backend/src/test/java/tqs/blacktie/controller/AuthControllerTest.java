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
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import tqs.blacktie.dto.LoginRequest;
import tqs.blacktie.dto.LoginResponse;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.SignUpResponse;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.UserService;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "hashedPassword");
        testUser.setId(1L);
        testUser.setRole("renter");
    }

    @Nested
    @DisplayName("Registration Endpoint Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should register user successfully")
        void whenValidRegister_thenReturnCreated() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "password123");
            when(userService.createUser(any(SignUpRequest.class))).thenReturn(testUser);

            ResponseEntity<?> response = authController.register(request);

            assertEquals(HttpStatus.CREATED, response.getStatusCode());
            assertInstanceOf(SignUpResponse.class, response.getBody());
            SignUpResponse body = (SignUpResponse) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("John Doe", body.getName());
            assertEquals("john@example.com", body.getEmail());
            assertEquals("Account created successfully", body.getMessage());
        }

        @Test
        @DisplayName("Should return bad request when email exists")
        @SuppressWarnings("unchecked")
        void whenEmailExists_thenReturnBadRequest() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "password123");
            when(userService.createUser(any(SignUpRequest.class)))
                    .thenThrow(new IllegalArgumentException("Email already exists"));

            ResponseEntity<?> response = authController.register(request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("Email already exists", body.get("message"));
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully")
        void whenValidLogin_thenReturnOk() {
            LoginRequest request = new LoginRequest("john@example.com", "password123");
            when(userService.authenticateUser("john@example.com", "password123")).thenReturn(testUser);

            ResponseEntity<?> response = authController.login(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(LoginResponse.class, response.getBody());
            LoginResponse body = (LoginResponse) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("John Doe", body.getName());
            assertEquals("john@example.com", body.getEmail());
            assertEquals("renter", body.getRole());
            assertEquals("Login successful", body.getMessage());
        }

        @Test
        @DisplayName("Should return unauthorized for invalid credentials")
        @SuppressWarnings("unchecked")
        void whenInvalidCredentials_thenReturnUnauthorized() {
            LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");
            when(userService.authenticateUser("john@example.com", "wrongpassword"))
                    .thenThrow(new IllegalArgumentException("Invalid email or password"));

            ResponseEntity<?> response = authController.login(request);

            assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("Invalid email or password", body.get("message"));
        }

        @Test
        @DisplayName("Should login as admin and return correct role")
        void whenAdminLogin_thenReturnAdminRole() {
            User adminUser = new User("Admin", "admin@example.com", "hashedPassword", "admin");
            adminUser.setId(2L);
            
            LoginRequest request = new LoginRequest("admin@example.com", "password123");
            when(userService.authenticateUser("admin@example.com", "password123")).thenReturn(adminUser);

            ResponseEntity<?> response = authController.login(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            LoginResponse body = (LoginResponse) response.getBody();
            assertEquals("admin", body.getRole());
        }

        @Test
        @DisplayName("Should login as owner and return correct role")
        void whenOwnerLogin_thenReturnOwnerRole() {
            User ownerUser = new User("Owner", "owner@example.com", "hashedPassword", "owner");
            ownerUser.setId(3L);
            
            LoginRequest request = new LoginRequest("owner@example.com", "password123");
            when(userService.authenticateUser("owner@example.com", "password123")).thenReturn(ownerUser);

            ResponseEntity<?> response = authController.login(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            LoginResponse body = (LoginResponse) response.getBody();
            assertEquals("owner", body.getRole());
        }
    }

    @Nested
    @DisplayName("Validation Exception Handler Tests")
    class ValidationExceptionHandlerTests {

        @Test
        @DisplayName("Should handle validation exceptions correctly")
        void whenValidationFails_thenReturnBadRequest() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError = new FieldError("signUpRequest", "email", "Email is required");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<Map<String, String>> response = authController.handleValidationExceptions(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Email is required", response.getBody().get("email"));
            assertEquals("Validation failed", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void whenMultipleValidationErrors_thenReturnAllErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError emailError = new FieldError("signUpRequest", "email", "Email is required");
            FieldError nameError = new FieldError("signUpRequest", "name", "Name is required");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(emailError, nameError));

            ResponseEntity<Map<String, String>> response = authController.handleValidationExceptions(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Email is required", response.getBody().get("email"));
            assertEquals("Name is required", response.getBody().get("name"));
            assertEquals("Validation failed", response.getBody().get("message"));
        }
    }
}
