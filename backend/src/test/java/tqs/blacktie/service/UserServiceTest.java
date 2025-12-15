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

    private User testUser;
    private static final String RAW_PASSWORD = "password123";
    private static final String HASHED_PASSWORD = "$2a$10$hashedPassword";

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", HASHED_PASSWORD);
        testUser.setId(1L);
        testUser.setRole("renter");
    }

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should create user with valid data")
        void whenRegisterWithValidData_thenCreateUser() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", RAW_PASSWORD);
            
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.createUser(request);

            assertNotNull(result);
            assertEquals("john@example.com", result.getEmail());
            assertEquals("John Doe", result.getName());
            assertEquals("renter", result.getRole());
            
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertEquals(HASHED_PASSWORD, userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void whenRegisterWithExistingEmail_thenThrowException() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", RAW_PASSWORD);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.createUser(request)
            );
            
            assertEquals("Email already exists", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Should hash password when creating user")
        void whenRegisterUser_thenPasswordIsHashed() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", RAW_PASSWORD);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            userService.createUser(request);

            verify(passwordEncoder).encode(RAW_PASSWORD);
            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            assertNotEquals(RAW_PASSWORD, userCaptor.getValue().getPassword());
        }

        @Test
        @DisplayName("Should set default role as renter")
        void whenRegisterUser_thenDefaultRoleIsRenter() {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", RAW_PASSWORD);
            when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.createUser(request);

            assertEquals("renter", result.getRole());
        }

        @Test
        @DisplayName("Should create user with special characters in name")
        void whenRegisterWithSpecialCharacters_thenCreateUser() {
            SignUpRequest request = new SignUpRequest("João Döe", "joao@example.com", RAW_PASSWORD);
            User specialUser = new User("João Döe", "joao@example.com", HASHED_PASSWORD);
            specialUser.setId(2L);
            
            when(userRepository.existsByEmail("joao@example.com")).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenReturn(specialUser);

            User result = userService.createUser(request);

            assertEquals("João Döe", result.getName());
        }
    }

    @Nested
    @DisplayName("User Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should authenticate user with valid credentials")
        void whenLoginWithValidCredentials_thenReturnUser() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser("john@example.com", RAW_PASSWORD);

            assertNotNull(result);
            assertEquals("john@example.com", result.getEmail());
            assertEquals("John Doe", result.getName());
            verify(passwordEncoder).matches(RAW_PASSWORD, HASHED_PASSWORD);
        }

        @Test
        @DisplayName("Should throw exception for invalid email")
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
        @DisplayName("Should throw exception for invalid password")
        void whenLoginWithInvalidPassword_thenThrowException() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpassword", HASHED_PASSWORD)).thenReturn(false);

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.authenticateUser("john@example.com", "wrongpassword")
            );
            
            assertEquals("Invalid email or password", exception.getMessage());
        }

        @Test
        @DisplayName("Should authenticate admin user")
        void whenLoginAsAdmin_thenReturnAdminUser() {
            User adminUser = new User("Admin", "admin@example.com", HASHED_PASSWORD, "admin");
            adminUser.setId(2L);
            
            when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser("admin@example.com", RAW_PASSWORD);

            assertEquals("admin", result.getRole());
        }

        @Test
        @DisplayName("Should authenticate owner user")
        void whenLoginAsOwner_thenReturnOwnerUser() {
            User ownerUser = new User("Owner", "owner@example.com", HASHED_PASSWORD, "owner");
            ownerUser.setId(3L);
            
            when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
            when(passwordEncoder.matches(RAW_PASSWORD, HASHED_PASSWORD)).thenReturn(true);

            User result = userService.authenticateUser("owner@example.com", RAW_PASSWORD);

            assertEquals("owner", result.getRole());
        }
    }

    @Nested
    @DisplayName("User Role Management Tests")
    class RoleManagementTests {

        @Test
        @DisplayName("Should set user role to renter")
        void whenSetRoleToRenter_thenSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "renter");

            assertEquals("renter", result.getRole());
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("Should set user role to owner")
        void whenSetRoleToOwner_thenSuccess() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "owner");

            assertEquals("owner", result.getRole());
        }

        @Test
        @DisplayName("Should handle case insensitive role")
        void whenSetRoleUpperCase_thenNormalize() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.setUserRole(1L, "OWNER");

            assertEquals("owner", result.getRole());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void whenSetRoleForNonExistentUser_thenThrowException() {
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
        void whenSetInvalidRole_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.setUserRole(1L, "manager")
            );
            
            assertEquals("Invalid role. Only 'renter' or 'owner' are allowed", exception.getMessage());
        }

        @Test
        @DisplayName("Should not allow setting admin role")
        void whenSetAdminRole_thenThrowException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.setUserRole(1L, "admin")
            );
            
            assertEquals("Cannot set role to admin", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("User Profile Management Tests")
    class ProfileManagementTests {

        @Test
        @DisplayName("Should update user profile with valid data")
        void whenUpdateProfile_thenSuccess() {
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Jane Doe", "+351912345678", "123 Main St", null
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertNotNull(result);
            assertEquals("Jane Doe", result.getName());
            assertEquals("+351912345678", result.getPhone());
            assertEquals("123 Main St", result.getAddress());
        }

        @Test
        @DisplayName("Should update business info for owner")
        void whenUpdateBusinessInfo_thenSuccess() {
            testUser.setRole("owner");
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Business Owner", "+351987654321", "456 Ave", "Premium suit rental"
            );

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertEquals("Premium suit rental", result.getBusinessInfo());
        }

        @Test
        @DisplayName("Should throw exception for non-existent user")
        void whenUpdateNonExistentUser_thenThrowException() {
            UpdateProfileRequest request = new UpdateProfileRequest("New Name", null, null, null);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.updateProfile(999L, request)
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should update only provided fields")
        void whenUpdatePartialData_thenUpdateOnlyProvided() {
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
        @DisplayName("Should not update name if blank")
        void whenUpdateWithBlankName_thenKeepOldName() {
            UpdateProfileRequest request = new UpdateProfileRequest("   ", "+351912345678", "Address", null);

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);

            User result = userService.updateProfile(1L, request);

            assertEquals("John Doe", result.getName());
        }
    }

    @Nested
    @DisplayName("User Retrieval Tests")
    class RetrievalTests {

        @Test
        @DisplayName("Should get all users")
        void whenGetAllUsers_thenReturnList() {
            User user2 = new User("Jane Doe", "jane@example.com", HASHED_PASSWORD);
            user2.setId(2L);
            
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));

            List<User> result = userService.getAllUsers();

            assertEquals(2, result.size());
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should get user by ID")
        void whenGetUserById_thenReturnUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            User result = userService.getUserById(1L);

            assertNotNull(result);
            assertEquals(1L, result.getId());
        }

        @Test
        @DisplayName("Should throw exception when user not found by ID")
        void whenGetUserByIdNotFound_thenThrowException() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> userService.getUserById(999L)
            );

            assertEquals("User not found", exception.getMessage());
        }

        @Test
        @DisplayName("Should get user by email")
        void whenGetUserByEmail_thenReturnUser() {
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            User result = userService.getUserByEmail("john@example.com");

            assertNotNull(result);
            assertEquals("john@example.com", result.getEmail());
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
        @DisplayName("Should check if email exists - true")
        void whenEmailExists_thenReturnTrue() {
            when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

            boolean result = userService.emailExists("john@example.com");

            assertTrue(result);
        }

        @Test
        @DisplayName("Should check if email exists - false")
        void whenEmailNotExists_thenReturnFalse() {
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

            boolean result = userService.emailExists("new@example.com");

            assertFalse(result);
        }

        @Test
        @DisplayName("Should handle null email gracefully")
        void whenAuthenticateWithNullEmail_thenThrowException() {
            assertThrows(IllegalArgumentException.class, () -> userService.authenticateUser(null, "password"));
        }

        @Test
        @DisplayName("Should normalize email to lowercase")
        void whenRegisterWithUppercaseEmail_thenNormalizeToLowercase() {
            SignUpRequest request = new SignUpRequest("Test User", "TEST@EXAMPLE.COM", RAW_PASSWORD);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(RAW_PASSWORD)).thenReturn(HASHED_PASSWORD);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            User result = userService.createUser(request);

            assertEquals("test@example.com", result.getEmail().toLowerCase());
        }
    }
}
