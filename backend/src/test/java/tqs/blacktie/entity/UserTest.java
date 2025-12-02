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
        @DisplayName("Should create user with default constructor")
        void whenDefaultConstructor_thenCreateUser() {
            User user = new User();

            assertNotNull(user.getCreatedAt());
            assertEquals("renter", user.getRole());
        }

        @Test
        @DisplayName("Should create user with name, email, password")
        void whenThreeArgConstructor_thenCreateUser() {
            User user = new User("John Doe", "john@example.com", "password123");

            assertEquals("John Doe", user.getName());
            assertEquals("john@example.com", user.getEmail());
            assertEquals("password123", user.getPassword());
            assertEquals("renter", user.getRole());
            assertNotNull(user.getCreatedAt());
        }

        @Test
        @DisplayName("Should create user with name, email, password, role")
        void whenFourArgConstructor_thenCreateUser() {
            User user = new User("John Doe", "john@example.com", "password123", "admin");

            assertEquals("John Doe", user.getName());
            assertEquals("john@example.com", user.getEmail());
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
        @DisplayName("Should get and set business info")
        void whenSetBusinessInfo_thenGetBusinessInfo() {
            User user = new User();
            user.setBusinessInfo("Premium rental service");

            assertEquals("Premium rental service", user.getBusinessInfo());
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
}
