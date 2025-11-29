package tqs.blacktie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceRoleTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("Test User", "test@example.com", "hashedPassword");
        testUser.setId(1L);
    }

    @Test
    void testSetUserRole_Success_Renter() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.setUserRole(1L, "renter");

        assertNotNull(result);
        assertEquals("renter", result.getRole());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testSetUserRole_Success_Owner() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.setUserRole(1L, "owner");

        assertNotNull(result);
        assertEquals("owner", result.getRole());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void testSetUserRole_Success_CaseInsensitive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = userService.setUserRole(1L, "OWNER");

        assertNotNull(result);
        assertEquals("owner", result.getRole());
    }

    @Test
    void testSetUserRole_Fails_UserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.setUserRole(999L, "owner")
        );
        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSetUserRole_Fails_InvalidRole() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.setUserRole(1L, "manager")
        );
        assertEquals("Invalid role. Only 'renter' or 'owner' are allowed", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testSetUserRole_Fails_AdminNotAllowed() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.setUserRole(1L, "admin")
        );
        assertEquals("Cannot set role to admin", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
        verify(userRepository, never()).save(any(User.class));
    }
}
