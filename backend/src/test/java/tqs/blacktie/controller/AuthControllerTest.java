package tqs.blacktie.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tqs.blacktie.dto.LoginRequest;
import tqs.blacktie.dto.SignUpRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.UserService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User("John Doe", "john@example.com", "hashedPassword");
        testUser.setId(1L);
        testUser.setRole("renter");
    }

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should return 201 when registration is successful")
        void whenRegisterWithValidData_thenReturn201() throws Exception {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "password123");
            
            when(userService.createUser(any(SignUpRequest.class))).thenReturn(testUser);

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.message").value("Account created successfully"));
        }

        @Test
        @DisplayName("Should return 400 when email already exists")
        void whenRegisterWithExistingEmail_thenReturn400() throws Exception {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "password123");
            
            when(userService.createUser(any(SignUpRequest.class)))
                .thenThrow(new IllegalArgumentException("Email already exists"));

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
        }

        @Test
        @DisplayName("Should return 400 when name is missing")
        void whenRegisterWithMissingName_thenReturn400() throws Exception {
            String invalidRequest = "{\"email\":\"john@example.com\",\"password\":\"password123\"}";

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name").value("Name is required"));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid")
        void whenRegisterWithInvalidEmail_thenReturn400() throws Exception {
            SignUpRequest request = new SignUpRequest("John Doe", "invalid-email", "password123");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email should be valid"));
        }

        @Test
        @DisplayName("Should return 400 when password is too short")
        void whenRegisterWithShortPassword_thenReturn400() throws Exception {
            SignUpRequest request = new SignUpRequest("John Doe", "john@example.com", "short");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password must be at least 8 characters long"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 when login is successful")
        void whenLoginWithValidCredentials_thenReturn200() throws Exception {
            LoginRequest request = new LoginRequest("john@example.com", "password123");
            
            when(userService.authenticateUser(anyString(), anyString())).thenReturn(testUser);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.role").value("renter"))
                .andExpect(jsonPath("$.message").value("Login successful"));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void whenLoginWithInvalidCredentials_thenReturn401() throws Exception {
            LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");
            
            when(userService.authenticateUser(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
        }

        @Test
        @DisplayName("Should return 400 when email is missing")
        void whenLoginWithMissingEmail_thenReturn400() throws Exception {
            String invalidRequest = "{\"password\":\"password123\"}";

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email is required"));
        }

        @Test
        @DisplayName("Should return 400 when password is missing")
        void whenLoginWithMissingPassword_thenReturn400() throws Exception {
            String invalidRequest = "{\"email\":\"john@example.com\"}";

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").value("Password is required"));
        }

        @Test
        @DisplayName("Should return 400 when email format is invalid")
        void whenLoginWithInvalidEmailFormat_thenReturn400() throws Exception {
            LoginRequest request = new LoginRequest("invalid-email", "password123");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email should be valid"));
        }
    }
}
