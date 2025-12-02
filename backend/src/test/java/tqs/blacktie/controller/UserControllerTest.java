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
import tqs.blacktie.dto.SetRoleRequest;
import tqs.blacktie.dto.UpdateProfileRequest;
import tqs.blacktie.entity.User;
import tqs.blacktie.service.UserService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@DisplayName("UserController Tests")
class UserControllerTest {

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
        testUser.setCreatedAt(LocalDateTime.of(2024, 1, 1, 12, 0));
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return list of users")
        void whenGetAllUsers_thenReturnUsersList() throws Exception {
            User user2 = new User("Jane Doe", "jane@example.com", "hashedPassword");
            user2.setId(2L);
            user2.setRole("owner");
            user2.setCreatedAt(LocalDateTime.of(2024, 1, 2, 12, 0));

            when(userService.getAllUsers()).thenReturn(Arrays.asList(testUser, user2));

            mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("John Doe"))
                .andExpect(jsonPath("$[0].role").value("renter"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Jane Doe"))
                .andExpect(jsonPath("$[1].role").value("owner"));
        }

        @Test
        @DisplayName("Should return empty list when no users")
        void whenGetAllUsersEmpty_thenReturnEmptyList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("POST /api/users/{userId}/role")
    class SetUserRoleTests {

        @Test
        @DisplayName("Should set user role to owner")
        void whenSetUserRoleToOwner_thenReturn200() throws Exception {
            SetRoleRequest request = new SetRoleRequest("owner");
            testUser.setRole("owner");
            
            when(userService.setUserRole(eq(1L), eq("owner"))).thenReturn(testUser);

            mockMvc.perform(post("/api/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.role").value("owner"));
        }

        @Test
        @DisplayName("Should set user role to renter")
        void whenSetUserRoleToRenter_thenReturn200() throws Exception {
            SetRoleRequest request = new SetRoleRequest("renter");
            
            when(userService.setUserRole(eq(1L), eq("renter"))).thenReturn(testUser);

            mockMvc.perform(post("/api/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("renter"));
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void whenSetUserRoleUserNotFound_thenReturn400() throws Exception {
            SetRoleRequest request = new SetRoleRequest("owner");
            
            when(userService.setUserRole(eq(999L), eq("owner")))
                .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(post("/api/users/999/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("Should return 400 when role is invalid")
        void whenSetUserRoleInvalid_thenReturn400() throws Exception {
            SetRoleRequest request = new SetRoleRequest("manager");

            mockMvc.perform(post("/api/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 when role is blank")
        void whenSetUserRoleBlank_thenReturn400() throws Exception {
            String invalidRequest = "{\"role\":\"\"}";

            mockMvc.perform(post("/api/users/1/role")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/users/{userId}/profile")
    class GetUserProfileTests {

        @Test
        @DisplayName("Should return user profile")
        void whenGetUserProfile_thenReturnProfile() throws Exception {
            testUser.setPhone("+351912345678");
            testUser.setAddress("123 Main St, Lisbon");
            
            when(userService.getUserById(1L)).thenReturn(testUser);

            mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.phone").value("+351912345678"))
                .andExpect(jsonPath("$.address").value("123 Main St, Lisbon"));
        }

        @Test
        @DisplayName("Should return 404 when user not found")
        void whenGetUserProfileNotFound_thenReturn404() throws Exception {
            when(userService.getUserById(999L))
                .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(get("/api/users/999/profile"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
        }
    }

    @Nested
    @DisplayName("PUT /api/users/{userId}/profile")
    class UpdateUserProfileTests {

        @Test
        @DisplayName("Should update user profile")
        void whenUpdateUserProfile_thenReturn200() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(
                "John Updated", "+351912345678", "123 Main St", null
            );
            
            testUser.setName("John Updated");
            testUser.setPhone("+351912345678");
            testUser.setAddress("123 Main St");
            
            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(testUser);

            mockMvc.perform(put("/api/users/1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John Updated"))
                .andExpect(jsonPath("$.phone").value("+351912345678"))
                .andExpect(jsonPath("$.address").value("123 Main St"));
        }

        @Test
        @DisplayName("Should return 400 when user not found")
        void whenUpdateUserProfileNotFound_thenReturn400() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest("John", null, null, null);
            
            when(userService.updateProfile(eq(999L), any(UpdateProfileRequest.class)))
                .thenThrow(new IllegalArgumentException("User not found"));

            mockMvc.perform(put("/api/users/999/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("Should return 400 when name is blank")
        void whenUpdateUserProfileBlankName_thenReturn400() throws Exception {
            String invalidRequest = "{\"name\":\"\"}";

            mockMvc.perform(put("/api/users/1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should update profile with business info for owner")
        void whenUpdateUserProfileWithBusinessInfo_thenReturn200() throws Exception {
            UpdateProfileRequest request = new UpdateProfileRequest(
                "Business Owner", "+351912345678", "Business St", "Premium rental service"
            );
            
            testUser.setRole("owner");
            testUser.setName("Business Owner");
            testUser.setBusinessInfo("Premium rental service");
            
            when(userService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(testUser);

            mockMvc.perform(put("/api/users/1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessInfo").value("Premium rental service"));
        }
    }
}
