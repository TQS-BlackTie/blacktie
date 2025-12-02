package tqs.blacktie.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;
import tqs.blacktie.service.UserService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private String rawPassword = "password123";
    private String hashedPassword = "$2a$10$hashedPassword";

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", hashedPassword);
        testUser.setId(1L);
        testUser.setRole("renter");
    }

    @Test
    void whenLoginWithValidCredentials_thenReturnUser() {
        
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

     
        User result = userService.authenticateUser("john@example.com", rawPassword);

       
        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getName());
        assertEquals("renter", result.getRole());
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(passwordEncoder, times(1)).matches(rawPassword, hashedPassword);
    }

    @Test
    void whenLoginWithInvalidEmail_thenThrowException() {
     
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

       
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateUser("nonexistent@example.com", rawPassword)
        );
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void whenLoginWithInvalidPassword_thenThrowException() {
     
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", hashedPassword)).thenReturn(false);

      
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateUser("john@example.com", "wrongpassword")
        );
        
        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository, times(1)).findByEmail("john@example.com");
        verify(passwordEncoder, times(1)).matches("wrongpassword", hashedPassword);
    }

    @Test
    void whenLoginWithEmptyEmail_thenThrowException() {
        
        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateUser("", rawPassword)
        );
        
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void whenLoginWithNullEmail_thenThrowException() {
    
        when(userRepository.findByEmail(null)).thenReturn(Optional.empty());

        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.authenticateUser(null, rawPassword)
        );
        
        assertEquals("Invalid email or password", exception.getMessage());
    }

    @Test
    void whenLoginWithAdminRole_thenReturnAdminUser() {
      
        User adminUser = new User("Admin User", "admin@example.com", hashedPassword, "admin");
        adminUser.setId(2L);
        
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

     
        User result = userService.authenticateUser("admin@example.com", rawPassword);

      
        assertNotNull(result);
        assertEquals("admin@example.com", result.getEmail());
        assertEquals("Admin User", result.getName());
        assertEquals("admin", result.getRole());
    }

    @Test
    void whenLoginWithOwnerRole_thenReturnOwnerUser() {
        
        User ownerUser = new User("Owner User", "owner@example.com", hashedPassword, "owner");
        ownerUser.setId(3L);
        
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(ownerUser));
        when(passwordEncoder.matches(rawPassword, hashedPassword)).thenReturn(true);

     
        User result = userService.authenticateUser("owner@example.com", rawPassword);

  
        assertNotNull(result);
        assertEquals("owner@example.com", result.getEmail());
        assertEquals("Owner User", result.getName());
        assertEquals("owner", result.getRole());
    }
}
