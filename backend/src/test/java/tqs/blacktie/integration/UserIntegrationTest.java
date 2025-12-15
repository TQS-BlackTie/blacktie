package tqs.blacktie.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        // Clean up all tables in order to respect FK constraints
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testCreateUserIntegration() {
        SignUpRequest request = new SignUpRequest();
        request.setName("John Doe");
        request.setEmail("john@test.com");
        request.setPassword("password123");

        User created = userService.createUser(request);

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("John Doe");
        assertThat(created.getEmail()).isEqualTo("john@test.com");
        assertThat(created.getRole()).isEqualTo("renter"); // default role
    }

    @Test
    void testCreateDuplicateEmailFailsIntegration() {
        SignUpRequest request1 = new SignUpRequest();
        request1.setName("User 1");
        request1.setEmail("duplicate@test.com");
        request1.setPassword("password123");
        userService.createUser(request1);

        SignUpRequest request2 = new SignUpRequest();
        request2.setName("User 2");
        request2.setEmail("duplicate@test.com");
        request2.setPassword("password456");

        assertThatThrownBy(() -> userService.createUser(request2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");
    }

    @Test
    void testAuthenticateUserIntegration() {
        SignUpRequest request = new SignUpRequest();
        request.setName("Auth User");
        request.setEmail("auth@test.com");
        request.setPassword("mypassword");
        userService.createUser(request);

        User authenticated = userService.authenticateUser("auth@test.com", "mypassword");

        assertThat(authenticated).isNotNull();
        assertThat(authenticated.getEmail()).isEqualTo("auth@test.com");
    }

    @Test
    void testAuthenticateWithWrongPasswordFailsIntegration() {
        SignUpRequest request = new SignUpRequest();
        request.setName("Test User");
        request.setEmail("test@test.com");
        request.setPassword("correctpassword");
        userService.createUser(request);

        assertThatThrownBy(() -> userService.authenticateUser("test@test.com", "wrongpassword"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    void testSetUserRoleIntegration() {
        SignUpRequest request = new SignUpRequest();
        request.setName("Role User");
        request.setEmail("role@test.com");
        request.setPassword("password");
        User user = userService.createUser(request);

        assertThat(user.getRole()).isEqualTo("renter");

        User updatedUser = userService.setUserRole(user.getId(), "owner");

        assertThat(updatedUser.getRole()).isEqualTo("owner");
    }

    @Test
    void testUpdateProfileIntegration() {
        SignUpRequest signUpRequest = new SignUpRequest();
        signUpRequest.setName("Original Name");
        signUpRequest.setEmail("profile@test.com");
        signUpRequest.setPassword("password");
        User user = userService.createUser(signUpRequest);

        UpdateProfileRequest updateRequest = new UpdateProfileRequest();
        updateRequest.setName("Updated Name");
        updateRequest.setPhone("123456789");
        updateRequest.setAddress("123 Test Street");

        User updated = userService.updateProfile(user.getId(), updateRequest);

        assertThat(updated.getName()).isEqualTo("Updated Name");
        assertThat(updated.getPhone()).isEqualTo("123456789");
        assertThat(updated.getAddress()).isEqualTo("123 Test Street");
    }

    @Test
    void testGetUserByIdIntegration() {
        SignUpRequest request = new SignUpRequest();
        request.setName("Find Me");
        request.setEmail("findme@test.com");
        request.setPassword("password");
        User created = userService.createUser(request);

        User found = userService.getUserById(created.getId());

        assertThat(found.getId()).isEqualTo(created.getId());
        assertThat(found.getEmail()).isEqualTo("findme@test.com");
    }
}
