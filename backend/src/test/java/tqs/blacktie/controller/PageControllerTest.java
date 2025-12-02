package tqs.blacktie.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PageController Tests")
class PageControllerTest {

    private PageController pageController;

    @BeforeEach
    void setUp() {
        pageController = new PageController();
    }

    @Nested
    @DisplayName("Home Endpoint Tests")
    class HomeEndpointTests {

        @Test
        @DisplayName("should return forward to index.html for home")
        void home_shouldReturnForwardToIndex() {
            String result = pageController.home();
            
            assertEquals("forward:/index.html", result);
        }
    }

    @Nested
    @DisplayName("Register Endpoint Tests")
    class RegisterEndpointTests {

        @Test
        @DisplayName("should return forward to index.html for register")
        void register_shouldReturnForwardToIndex() {
            String result = pageController.register();
            
            assertEquals("forward:/index.html", result);
        }
    }

    @Nested
    @DisplayName("Login Endpoint Tests")
    class LoginEndpointTests {

        @Test
        @DisplayName("should return forward to index.html for login")
        void login_shouldReturnForwardToIndex() {
            String result = pageController.login();
            
            assertEquals("forward:/index.html", result);
        }
    }

    @Nested
    @DisplayName("Signup Endpoint Tests")
    class SignupEndpointTests {

        @Test
        @DisplayName("should return forward to index.html for signup")
        void signup_shouldReturnForwardToIndex() {
            String result = pageController.signup();
            
            assertEquals("forward:/index.html", result);
        }
    }
}
