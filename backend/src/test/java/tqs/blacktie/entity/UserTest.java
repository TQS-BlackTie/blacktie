package tqs.blacktie.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User Entity Tests")
class UserTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create user with no-args constructor")
        void whenNoArgsConstructor_thenCreateUser() {
            User user = new User();

            assertNotNull(user);
            assertEquals("renter", user.getRole());
            assertNotNull(user.getCreatedAt());
        }

        @Test
        @DisplayName("Should create user with 3-args constructor")
        void whenThreeArgsConstructor_thenCreateUser() {
            User user = new User("John Doe", "john@example.com", "password123");

            assertEquals("John Doe", user.getName());
            assertEquals("john@example.com", user.getEmail());
            assertEquals("password123", user.getPassword());
            assertEquals("renter", user.getRole());
            assertNotNull(user.getCreatedAt());
        }

        @Test
        @DisplayName("Should create user with 4-args constructor")
        void whenFourArgsConstructor_thenCreateUser() {
            User user = new User("Admin", "admin@example.com", "password123", "admin");

            assertEquals("Admin", user.getName());
            assertEquals("admin@example.com", user.getEmail());
            assertEquals("password123", user.getPassword());
            assertEquals("admin", user.getRole());
            assertNotNull(user.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Getter and Setter Tests")
    class GetterSetterTests {

        @Test
        @DisplayName("Should get and set id")
        void whenSetId_thenGetId() {
            User user = new User();
            user.setId(1L);

            assertEquals(1L, user.getId());
        }

        @Test
        @DisplayName("Should get and set name")
        void whenSetName_thenGetName() {
            User user = new User();
            user.setName("Jane Doe");

            assertEquals("Jane Doe", user.getName());
        }

        @Test
        @DisplayName("Should get and set email")
        void whenSetEmail_thenGetEmail() {
            User user = new User();
            user.setEmail("jane@example.com");

            assertEquals("jane@example.com", user.getEmail());
        }

        @Test
        @DisplayName("Should get and set password")
        void whenSetPassword_thenGetPassword() {
            User user = new User();
            user.setPassword("newPassword");

            assertEquals("newPassword", user.getPassword());
        }

        @Test
        @DisplayName("Should get and set role")
        void whenSetRole_thenGetRole() {
            User user = new User();
            user.setRole("owner");

            assertEquals("owner", user.getRole());
        }

        @Test
        @DisplayName("Should get and set phone")
        void whenSetPhone_thenGetPhone() {
            User user = new User();
            user.setPhone("+351912345678");

            assertEquals("+351912345678", user.getPhone());
        }

        @Test
        @DisplayName("Should get and set address")
        void whenSetAddress_thenGetAddress() {
            User user = new User();
            user.setAddress("123 Main St, Lisbon");

            assertEquals("123 Main St, Lisbon", user.getAddress());
        }

        @Test
        @DisplayName("Should get and set businessInfo")
        void whenSetBusinessInfo_thenGetBusinessInfo() {
            User user = new User();
            user.setBusinessInfo("Premium suit rental service");

            assertEquals("Premium suit rental service", user.getBusinessInfo());
        }

        @Test
        @DisplayName("Should get and set createdAt")
        void whenSetCreatedAt_thenGetCreatedAt() {
            User user = new User();
            LocalDateTime now = LocalDateTime.now();
            user.setCreatedAt(now);

            assertEquals(now, user.getCreatedAt());
        }
    }

    @Nested
    @DisplayName("Default Values Tests")
    class DefaultValuesTests {

        @Test
        @DisplayName("Default role should be renter")
        void whenNewUser_thenDefaultRoleIsRenter() {
            User user = new User("Test", "test@example.com", "password");

            assertEquals("renter", user.getRole());
        }

        @Test
        @DisplayName("CreatedAt should be set on construction")
        void whenNewUser_thenCreatedAtIsSet() {
            LocalDateTime before = LocalDateTime.now().minusSeconds(1);
            User user = new User("Test", "test@example.com", "password");
            LocalDateTime after = LocalDateTime.now().plusSeconds(1);

            assertNotNull(user.getCreatedAt());
            assertTrue(user.getCreatedAt().isAfter(before));
            assertTrue(user.getCreatedAt().isBefore(after));
        }

        @Test
        @DisplayName("Optional fields should be null initially")
        void whenNewUser_thenOptionalFieldsAreNull() {
            User user = new User("Test", "test@example.com", "password");

            assertNull(user.getId());
            assertNull(user.getPhone());
            assertNull(user.getAddress());
            assertNull(user.getBusinessInfo());
        }
    }
}
