package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPassword";
    private static final String TEST_EMAIL = "john@example.com";
    private static final String TEST_NAME = "John Doe";

    private User createTestUser() {
        User user = new User(TEST_NAME, TEST_EMAIL, HASHED_PASSWORD);
        user.setId(1L);
        user.setRole("renter");
        return user;
    }

    private User createTestUserWithRole(String name, String email, String role) {
        User user = new User(name, email, HASHED_PASSWORD, role);
        user.setId(1L);
        return user;
    }

    @Nested
    @DisplayName("Registration Tests")
    class RegistrationTests {

        private SignUpRequest validRequest;

        @BeforeEach
        void setUp() {
            validRequest = new SignUpRequest(TEST_NAME, TEST_EMAIL, RAW_PASSWORD);
        }

        @Test
        @DisplayName("Should create user with valid data")
        void whenRegisterWithValidData_thenCreateUser() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            
            User savedUser = createTestUser();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.createUser(validRequest);

            assertNotNull(result);
            assertEquals(TEST_EMAIL, result.getEmail());
            assertEquals(TEST_NAME, result.getName());
            assertEquals("renter", result.getRole());
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(HASHED_PASSWORD, userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void whenRegisterWithExistingEmail_thenThrowException() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(validRequest)
            );
            
            assertEquals("Email already exists", exception.getMessage());
            verify(passwordEncoder, never()).encode(anyString());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should hash password before saving")
        void whenRegisterUser_thenPasswordIsHashed() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(createTestUser());

            userService.createUser(validRequest);

            verify(passwordEncoder).encode(RAW_PASSWORD);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertNotEquals(RAW_PASSWORD, userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should set default role as renter")
        void whenRegisterUser_thenDefaultRoleIsRenter() {
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            
            User savedUser = createTestUser();
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.createUser(validRequest);

            assertEquals("renter", result.getRole());
        }

        @Test
        @DisplayName("Should handle special characters in name")
        void whenRegisterWithSpecialCharactersInName_thenCreateUser() {
            SignUpRequest request = new SignUpRequest("João Doe", TEST_EMAIL, RAW_PASSWORD);
            when(userRepository.existsByEmail(TEST_EMAIL)).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            
            User savedUser = new User("João Doe", TEST_EMAIL, HASHED_PASSWORD);
            savedUser.setId(1L);
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            User result = userService.createUser(request);

            assertEquals("João Doe", result.getName());
        }
    }

    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {

        private User testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser();
        }

        @Test
        @DisplayName("Should authenticate user with valid credentials")
        void whenLoginWithValidCredentials_thenReturnUser() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser(TEST_EMAIL, RAW_PASSWORD);

            assertNotNull(result);
            assertEquals(TEST_EMAIL, result.getEmail());
            assertEquals(TEST_NAME, result.getName());
            verify(userRepository).findByEmail(TEST_EMAIL);
            verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
        }

        @Test
        @DisplayName("Should throw exception when email not found")
        void whenLoginWithInvalidEmail_thenThrowException() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticateUser("nonexistent@example.com", RAW_PASSWORD)
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
            verify(passwordEncoder, never()).matches(anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw exception when password is invalid")
        void whenLoginWithInvalidPassword_thenThrowException() {
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", HASHED_PASSWORD)).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticateUser(TEST_EMAIL, "wrongpassword")
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Should authenticate admin user")
        void whenLoginWithAdminRole_thenReturnAdminUser() {
            User adminUser = createTestUserWithRole("Admin User", "admin@example.com", "admin");
            
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser("admin@example.com", RAW_PASSWORD);

            assertEquals("admin", result.getRole());
        }

        @Test
        @DisplayName("Should authenticate owner user")
        void whenLoginWithOwnerRole_thenReturnOwnerUser() {
            User ownerUser = createTestUserWithRole("Owner User", "owner@example.com", "owner");
            
            when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser("owner@example.com", RAW_PASSWORD);

            assertEquals("owner", result.getRole());
        }

        @Test
        @DisplayName("Should throw exception when email is empty")
        void whenLoginWithEmptyEmail_thenThrowException() {
            when(userRepository.findByEmail("")).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                () -> userService.authenticateUser("", RAW_PASSWORD));
        }

        @Test
        @DisplayName("Should throw exception when email is null")
        void whenLoginWithNullEmail_thenThrowException() {
            when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

            assertThrows(IllegalArgumentException.class,
                () -> userService.authenticateUser(null, RAW_PASSWORD));
        }
    }

    @Nested
    @DisplayName("Role Management Tests")
    class RoleManagementTests {

        private User testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser();
        }

        @Test
        @DisplayName("Should set role to renter")
        void whenSetUserRoleToRenter_thenSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "renter");

            assertEquals("renter", result.getRole());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should set role to owner")
        void whenSetUserRoleToOwner_thenSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "owner");

            assertEquals("owner", result.getRole());
        }

        @Test
        @DisplayName("Should handle case insensitive role")
        void whenSetUserRoleCaseInsensitive_thenSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "OWNER");

            assertEquals("owner", result.getRole());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void whenSetUserRoleUserNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.setUserRole(999L, "owner")
            );
            
            assertEquals("User not found", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid role")
        void whenSetUserRoleInvalidRole_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.setUserRole(1L, "manager")
            );
            
            assertEquals("Invalid role. Only 'renter' or 'owner' are allowed", exception.getMessage());
        }

        @Test
        @DisplayName("Should not allow setting role to admin")
        void whenSetUserRoleToAdmin_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.setUserRole(1L, "admin")
            );
            
            assertEquals("Cannot set role to admin", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Profile Management Tests")
    class ProfileManagementTests {

        private User testUser;

        @BeforeEach
        void setUp() {
            testUser = createTestUser();
        }

        @Test
        @DisplayName("Should update profile with valid data")
        void whenUpdateProfileWithValidData_thenSuccess() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe", "+351912345678", "123 Main St, Lisbon", null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertNotNull(result);
            assertEquals("Jane Doe", result.getName());
            assertEquals("+351912345678", result.getPhone());
            assertEquals("123 Main St, Lisbon", result.getAddress());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should update business info for owner")
        void whenUpdateProfileWithBusinessInfo_thenSuccess() {
            testUser.setRole("owner");
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Business Owner", "+351987654321", "456 Business Ave", "Premium suit rental service"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertEquals("Business Owner", result.getName());
            assertEquals("Premium suit rental service", result.getBusinessInfo());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void whenUpdateProfileUserNotFound_thenThrowException() {
            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null, null, null);

            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(999L, request)
            );

            assertEquals("User not found", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should only update provided fields")
        void whenUpdateProfilePartialData_thenUpdateOnlyProvidedFields() {
            testUser.setPhone("+351999999999");
            testUser.setAddress("Old Address");

            UpdateProfileRequest request = new UpdateProfileRequest("Updated Name", null, null, null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertEquals("Updated Name", result.getName());
            assertEquals("+351999999999", result.getPhone());
            assertEquals("Old Address", result.getAddress());
        }

        @Test
        @DisplayName("Should not update name when empty string")
        void whenUpdateProfileEmptyName_thenNotUpdate() {
            UpdateProfileRequest request = new UpdateProfileRequest("   ", "+351912345678", "New Address", null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertEquals(TEST_NAME, result.getName());
            assertEquals("+351912345678", result.getPhone());
        }
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class UserRetrievalTests {

        @Test
        @DisplayName("Should return all users")
        void whenGetAllUsers_thenReturnAllUsers() {
            User user1 = createTestUserWithRole("User 1", "user1@example.com", "renter");
            User user2 = createTestUserWithRole("User 2", "user2@example.com", "owner");
            
            when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

            List<User> result = userService.getAllUsers();

            assertEquals(2, result.size());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void whenGetAllUsersEmpty_thenReturnEmptyList() {
            when(userRepository.findAll()).thenReturn(List.of());

            List<User> result = userService.getAllUsers();

            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return user by id")
        void whenGetUserById_thenReturnUser() {
            User testUser = createTestUser();
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should throw exception when user not found by id")
        void whenGetUserByIdNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(999L)
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should return user by email")
        void whenGetUserByEmail_thenReturnUser() {
            User testUser = createTestUser();
            when(userRepository.findByEmail(TEST_EMAIL)).thenReturn(Optional.of(testUser));

            User result = userService.getUserByEmail(TEST_EMAIL);

            assertNotNull(result);
            assertEquals(TEST_EMAIL, result.getEmail());
        }

        @Test
        @DisplayName("Should throw exception when user not found by email")
        void whenGetUserByEmailNotFound_thenThrowException() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserByEmail("nonexistent@example.com")
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should check if email exists")
        void whenEmailExists_thenReturnTrue() {
            when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

            boolean result = userService.emailExists("existing@example.com");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should check if email does not exist")
        void whenEmailNotExists_thenReturnFalse() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            boolean result = userService.emailExists("new@example.com");

            assertFalse(result);
        }
    }
}
