package tqs.blacktie.e2e.cucumber;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;

import org.springframework.beans.factory.annotation.Autowired;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.repository.*;
import tqs.blacktie.service.UserService;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAuthSteps {

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

    private User lastCreatedUser;
    private User lastAuthenticatedUser;
    private Exception lastException;

    @Before("@auth")
    public void setUp() {
        // Clean up for auth tests
        notificationRepository.deleteAll();
        reviewRepository.deleteAll();
        bookingRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        lastCreatedUser = null;
        lastAuthenticatedUser = null;
        lastException = null;
    }

    @Given("no user exists with email {string}")
    public void noUserExistsWithEmail(String email) {
        userRepository.findByEmail(email).ifPresent(u -> userRepository.delete(u));
    }

    @Given("a user exists with email {string}")
    public void aUserExistsWithEmail(String email) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User user = new User("Test User", email, "hashedpassword", "renter");
            userRepository.save(user);
        }
    }

    @Given("a user exists with email {string} and password {string}")
    public void aUserExistsWithEmailAndPassword(String email, String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            SignUpRequest request = new SignUpRequest();
            request.setName("Test User");
            request.setEmail(email);
            request.setPassword(password);
            userService.createUser(request);
        }
    }

    @Given("a user exists with email {string} and role {string}")
    public void aUserExistsWithEmailAndRole(String email, String role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User("Test User", email, "hashedpassword", role);
            return userRepository.save(newUser);
        });
        if (!user.getRole().equals(role)) {
            user.setRole(role);
            userRepository.save(user);
        }
        lastCreatedUser = user;
    }

    @When("I register with name {string} and email {string} and password {string}")
    public void iRegisterWithNameAndEmailAndPassword(String name, String email, String password) {
        try {
            SignUpRequest request = new SignUpRequest();
            request.setName(name);
            request.setEmail(email);
            request.setPassword(password);
            lastCreatedUser = userService.createUser(request);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("I try to register with email {string}")
    public void iTryToRegisterWithEmail(String email) {
        try {
            SignUpRequest request = new SignUpRequest();
            request.setName("Duplicate User");
            request.setEmail(email);
            request.setPassword("SomePassword123");
            lastCreatedUser = userService.createUser(request);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("I login with email {string} and password {string}")
    public void iLoginWithEmailAndPassword(String email, String password) {
        try {
            lastAuthenticatedUser = userService.authenticateUser(email, password);
            lastException = null;
        } catch (Exception e) {
            lastException = e;
        }
    }

    @When("I change the role of {string} to {string}")
    public void iChangeTheRoleOfTo(String email, String newRole) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        lastCreatedUser = userService.setUserRole(user.getId(), newRole);
    }

    @Then("the user {string} should exist in the system")
    public void theUserShouldExistInTheSystem(String email) {
        assertThat(userRepository.findByEmail(email)).isPresent();
    }

    @Then("the user role should be {string}")
    public void theUserRoleShouldBe(String expectedRole) {
        assertThat(lastCreatedUser).isNotNull();
        assertThat(lastCreatedUser.getRole()).isEqualTo(expectedRole);
    }

    @Then("the registration should fail with message {string}")
    public void theRegistrationShouldFailWithMessage(String message) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).contains(message);
    }

    @Then("I should be authenticated successfully")
    public void iShouldBeAuthenticatedSuccessfully() {
        assertThat(lastException).isNull();
        assertThat(lastAuthenticatedUser).isNotNull();
    }

    @Then("the login should fail with message {string}")
    public void theLoginShouldFailWithMessage(String message) {
        assertThat(lastException).isNotNull();
        assertThat(lastException.getMessage()).contains(message);
    }
}
