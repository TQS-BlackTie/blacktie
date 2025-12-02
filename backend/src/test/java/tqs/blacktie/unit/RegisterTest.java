package tqs.blacktie.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;
import tqs.blacktie.service.UserService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private SignUpRequest validRequest;
    private String rawPassword = "password123";
    private String hashedPassword = "$2a$10$hashedPassword";

    @BeforeEach
    void setUp() {
        validRequest = new SignUpRequest("John Doe", "john@example.com", rawPassword);
    }

    @Test
    void whenRegisterWithValidData_thenCreateUser() {
      
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        
        User savedUser = new User("John Doe", "john@example.com", hashedPassword);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

      
        User result = userService.createUser(validRequest);

        
        assertNotNull(result);
        assertEquals("john@example.com", result.getEmail());
        assertEquals("John Doe", result.getName());
        assertEquals("renter", result.getRole());
        
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        User capturedUser = userCaptor.getValue();
        assertEquals(hashedPassword, capturedUser.getPassword());
        assertEquals("John Doe", capturedUser.getName());
        assertEquals("john@example.com", capturedUser.getEmail());
    }

    @Test
    void whenRegisterWithExistingEmail_thenThrowException() {
        
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

  
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(validRequest)
        );
        
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).existsByEmail("john@example.com");
        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void whenRegisterUser_thenPasswordIsHashed() {
      
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        
        User savedUser = new User("John Doe", "john@example.com", hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

       
        userService.createUser(validRequest);

       
        verify(passwordEncoder, times(1)).encode(rawPassword);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());
        
        User capturedUser = userCaptor.getValue();
        assertEquals(hashedPassword, capturedUser.getPassword());
        assertNotEquals(rawPassword, capturedUser.getPassword());
    }

    @Test
    void whenRegisterUser_thenDefaultRoleIsRenter() {
     
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        
        User savedUser = new User("John Doe", "john@example.com", hashedPassword);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

     
        User result = userService.createUser(validRequest);

        
        assertEquals("renter", result.getRole());
    }

    @Test
    void whenRegisterWithDifferentEmails_thenAllSucceed() {
        
        SignUpRequest request1 = new SignUpRequest("User One", "user1@example.com", "password1");
        SignUpRequest request2 = new SignUpRequest("User Two", "user2@example.com", "password2");
        
        when(userRepository.existsByEmail("user1@example.com")).thenReturn(false);
        when(userRepository.existsByEmail("user2@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn(hashedPassword);
        
        User savedUser1 = new User("User One", "user1@example.com", hashedPassword);
        User savedUser2 = new User("User Two", "user2@example.com", hashedPassword);
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser1, savedUser2);

       
        User result1 = userService.createUser(request1);
        User result2 = userService.createUser(request2);

       
        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals("user1@example.com", result1.getEmail());
        assertEquals("user2@example.com", result2.getEmail());
        verify(userRepository, times(2)).save(any(User.class));
    }

    @Test
    void whenRegisterWithSpecialCharactersInName_thenCreateUser() {
        
        SignUpRequest request = new SignUpRequest("João Doe", "john@example.com", rawPassword);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        
        User savedUser = new User("João Doe", "john@example.com", hashedPassword);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

     
        User result = userService.createUser(request);

        assertNotNull(result);
        assertEquals("João Doe", result.getName());
    }

    @Test
    void whenCheckEmailExists_thenReturnTrue() {
        
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

       
        boolean result = userService.emailExists("existing@example.com");

       
        assertTrue(result);
        verify(userRepository, times(1)).existsByEmail("existing@example.com");
    }

    @Test
    void whenCheckEmailNotExists_thenReturnFalse() {
        
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);

       
        boolean result = userService.emailExists("new@example.com");

       
        assertFalse(result);
        verify(userRepository, times(1)).existsByEmail("new@example.com");
    }

    @Test
    void whenRegisterMultipleUsersWithSameEmail_thenOnlyFirstSucceeds() {
       
        SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", rawPassword);
        
        when(userRepository.existsByEmail("john@example.com"))
            .thenReturn(false)  // First call
            .thenReturn(true);  // Second call
        
        when(passwordEncoder.encode(rawPassword)).thenReturn(hashedPassword);
        User savedUser = new User("John Doe", "john@example.com", hashedPassword);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

      
        User firstUser = userService.createUser(request);
        
      
        assertNotNull(firstUser);
        
 
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userService.createUser(request)
        );
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, times(1)).save(any(User.class));
    }
}
