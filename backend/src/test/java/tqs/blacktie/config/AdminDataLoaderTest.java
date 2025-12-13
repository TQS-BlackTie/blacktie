package tqs.blacktie.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.UserRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminDataLoader Tests")
class AdminDataLoaderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminDataLoader adminDataLoader;

    @BeforeEach
    void setUp() {
        adminDataLoader = new AdminDataLoader(userRepository, passwordEncoder);
        ReflectionTestUtils.setField(adminDataLoader, "adminPassword", "testpassword");
    }

    @Test
    @DisplayName("Should not create admin if already exists")
    void whenAdminExists_thenDoNothing() {
        User existingAdmin = new User("Admin", "admin@blacktie.pt", "encoded", User.ROLE_ADMIN);
        when(userRepository.findByEmail("admin@blacktie.pt")).thenReturn(Optional.of(existingAdmin));

        adminDataLoader.run();

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update old admin to new email")
    void whenOldAdminExists_thenUpdateEmail() {
        User oldAdmin = new User("Admin", "admin", "oldencoded", User.ROLE_ADMIN);
        oldAdmin.setId(1L);
        
        when(userRepository.findByEmail("admin@blacktie.pt")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin")).thenReturn(Optional.of(oldAdmin));
        when(passwordEncoder.encode("testpassword")).thenReturn("newencoded");

        adminDataLoader.run();

        verify(userRepository).save(argThat(user -> 
            "admin@blacktie.pt".equals(user.getEmail()) && 
            "newencoded".equals(user.getPassword())
        ));
    }

    @Test
    @DisplayName("Should create new admin if none exists")
    void whenNoAdminExists_thenCreateNew() {
        when(userRepository.findByEmail("admin@blacktie.pt")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("admin")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("testpassword")).thenReturn("encoded");

        adminDataLoader.run();

        verify(userRepository).save(argThat(user -> 
            "Platform Admin".equals(user.getName()) &&
            "admin@blacktie.pt".equals(user.getEmail()) &&
            User.ROLE_ADMIN.equals(user.getRole())
        ));
    }
}
