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
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.dto.UserResponse;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.UserService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Tests")
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "hashedPassword");
        testUser.setId(1L);
        testUser.setRole("renter");
        testUser.setPhone("+351912345678");
        testUser.setAddress("123 Main St");
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return list of users")
        void whenGetAllUsers_thenReturnList() {
            User user2 = new User("Jane Doe", "jane@example.com", "hashedPassword");
            user2.setId(2L);
            user2.setRole("owner");

            when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser, user2));

            List<UserResponse> result = userController.getAllUsers();

            assertEquals(2, result.size());
            assertEquals(1L, result.get(0).getId());
            assertEquals("John Doe", result.get(0).getName());
            assertEquals("renter", result.get(0).getRole());
            assertEquals(2L, result.get(1).getId());
            assertEquals("Jane Doe", result.get(1).getName());
            assertEquals("owner", result.get(1).getRole());
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void whenNoUsers_thenReturnEmptyList() {
            when(userService.getAllUsers()).thenReturn(Collections.emptyList());

            List<UserResponse> result = userController.getAllUsers();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Set User Role Tests")
    class SetUserRoleTests {

        @Test
        @DisplayName("Should set user role successfully")
        void whenSetRole_thenReturnUpdatedUser() {
            testUser.setRole("owner");
            SetRoleRequest request = new SetRoleRequest("owner");

            when(userService.setUserRole(eq(1L), eq("owner"))).thenReturn(testUser);

            ResponseEntity<?> response = userController.setUserRole(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(UserResponse.class, response.getBody());
            UserResponse body = (UserResponse) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("owner", body.getRole());
        }

        @Test
        @DisplayName("Should return bad request when user not found")
        @SuppressWarnings("unchecked")
        void whenUserNotFound_thenReturnBadRequest() {
            SetRoleRequest request = new SetRoleRequest("owner");

            when(userService.setUserRole(eq(999L), eq("owner")))
                    .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = userController.setUserRole(999L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("User not found", body.get("message"));
        }

        @Test
        @DisplayName("Should return bad request for invalid role")
        @SuppressWarnings("unchecked")
        void whenInvalidRole_thenReturnBadRequest() {
            SetRoleRequest request = new SetRoleRequest("admin");

            when(userService.setUserRole(eq(1L), eq("admin")))
                    .thenThrow(new IllegalArgumentException("Cannot set role to admin"));

            ResponseEntity<?> response = userController.setUserRole(1L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("Cannot set role to admin", body.get("message"));
        }
    }

    @Nested
    @DisplayName("Get User Profile Tests")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should get user profile successfully")
        void whenGetProfile_thenReturnUser() {
            when(userService.getUserById(1L)).thenReturn(testUser);

            ResponseEntity<?> response = userController.getUserProfile(1L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(UserResponse.class, response.getBody());
            UserResponse body = (UserResponse) response.getBody();
            assertEquals(1L, body.getId());
            assertEquals("John Doe", body.getName());
            assertEquals("john@example.com", body.getEmail());
            assertEquals("+351912345678", body.getPhone());
            assertEquals("123 Main St", body.getAddress());
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        @SuppressWarnings("unchecked")
        void whenUserNotFound_thenReturnNotFound() {
            when(userService.getUserById(999L))
                    .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = userController.getUserProfile(999L);

            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("User not found", body.get("message"));
        }
    }

    @Nested
    @DisplayName("Update User Profile Tests")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should update profile successfully")
        void whenUpdateProfile_thenReturnUpdatedUser() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Jane Doe", "+351987654321", "456 New St", null
            );

            testUser.setName("Jane Doe");
            testUser.setPhone("+351987654321");
            testUser.setAddress("456 New St");

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(testUser);

            ResponseEntity<?> response = userController.updateUserProfile(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertInstanceOf(UserResponse.class, response.getBody());
            UserResponse body = (UserResponse) response.getBody();
            assertEquals("Jane Doe", body.getName());
            assertEquals("+351987654321", body.getPhone());
            assertEquals("456 New St", body.getAddress());
        }

        @Test
        @DisplayName("Should return bad request when user not found")
        @SuppressWarnings("unchecked")
        void whenUserNotFound_thenReturnBadRequest() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Jane Doe", null, null, null
            );

            when(userService.updateProfile(eq(999L), any(UpdateProfileRequest.class)))
                    .thenThrow(new IllegalArgumentException("User not found"));

            ResponseEntity<?> response = userController.updateUserProfile(999L, request);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            Map<String, String> body = (Map<String, String>) response.getBody();
            assertEquals("User not found", body.get("message"));
        }

        @Test
        @DisplayName("Should update profile with business info for owner")
        void whenUpdateWithBusinessInfo_thenSuccess() {
            testUser.setRole("owner");
            UpdateProfileRequest request = new UpdateProfileRequest(
                    "Business Owner", "+351912345678", "Business St", "Premium service"
            );

            testUser.setName("Business Owner");
            testUser.setBusinessInfo("Premium service");

            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(testUser);

            ResponseEntity<?> response = userController.updateUserProfile(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            UserResponse body = (UserResponse) response.getBody();
            assertEquals("Premium service", body.getBusinessInfo());
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
            FieldError fieldError = new FieldError("updateProfileRequest", "name", "Name is required");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

            ResponseEntity<Map<String, String>> response = userController.handleValidationExceptions(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Name is required", response.getBody().get("name"));
            assertEquals("Validation failed", response.getBody().get("message"));
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void whenMultipleValidationErrors_thenReturnAllErrors() {
            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError roleError = new FieldError("setRoleRequest", "role", "Role is required");
            FieldError nameError = new FieldError("updateProfileRequest", "name", "Name is required");
            
            when(ex.getBindingResult()).thenReturn(bindingResult);
            when(bindingResult.getAllErrors()).thenReturn(List.of(roleError, nameError));

            ResponseEntity<Map<String, String>> response = userController.handleValidationExceptions(ex);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals("Role is required", response.getBody().get("role"));
            assertEquals("Name is required", response.getBody().get("name"));
            assertEquals("Validation failed", response.getBody().get("message"));
        }
    }
}
