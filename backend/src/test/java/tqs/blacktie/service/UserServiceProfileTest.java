package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "password123");
        testUser.setId(1L);
        testUser.setRole("renter");
    }

    @Test
    void updateProfile_withValidData_shouldUpdateUser() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Jane Doe",
            "+351912345678",
            "123 Main St, Lisbon",
            null
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
    void updateProfile_withBusinessInfo_shouldUpdateOwnerFields() {
        testUser.setRole("owner");
        UpdateProfileRequest request = new UpdateProfileRequest(
            "Business Owner",
            "+351987654321",
            "456 Business Ave",
            "Premium suit rental service"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateProfile(1L, request);

        assertNotNull(result);
        assertEquals("Business Owner", result.getName());
        assertEquals("Premium suit rental service", result.getBusinessInfo());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_withNonexistentUser_shouldThrowException() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "New Name",
            null,
            null,
            null
        );

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.updateProfile(999L, request)
        );

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateProfile_withPartialData_shouldUpdateOnlyProvidedFields() {
        testUser.setPhone("+351999999999");
        testUser.setAddress("Old Address");

        UpdateProfileRequest request = new UpdateProfileRequest(
            "Updated Name",
            null,
            null,
            null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateProfile(1L, request);

        assertNotNull(result);
        assertEquals("Updated Name", result.getName());
        assertEquals("+351999999999", result.getPhone());
        assertEquals("Old Address", result.getAddress());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateProfile_withEmptyStringName_shouldNotUpdate() {
        UpdateProfileRequest request = new UpdateProfileRequest(
            "   ",
            "+351912345678",
            "New Address",
            null
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.updateProfile(1L, request);

        assertEquals("John Doe", result.getName());
        assertEquals("+351912345678", result.getPhone());
        verify(userRepository).save(testUser);
    }
}
