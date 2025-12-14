package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for user login functionality
 */
class UserLoginE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testUserLogin() {
        // First register a user via API (faster setup)
        String name = uniqueName();
        String email = uniqueEmail();
        String password = "TestPassword123!";
        
        APIResponse registerResponse = registerUserViaAPI(name, email, password);
        assertEquals(201, registerResponse.status(), "Registration should succeed");
        
        // Now test login via UI
        navigateToLogin();
        
        // Fill login form
        fillInputById("email", email);
        fillInputById("password", password);
        
        // Submit login form directly to ensure it's submitted
        page.locator("form").locator("button[type='submit']").click();
        
        // Wait for login API call and redirect (1000ms timeout in frontend + navigation)
        page.waitForTimeout(4000);
        
        // Verify successful login - should redirect to role-setup
        assertTrue(
            page.url().contains("/role-setup"),
            "Expected redirect to role-setup after login. Current URL: " + page.url()
        );
    }
    
    @Test
    void testLoginWithInvalidCredentials() {
        navigateToLogin();
        
        // Try to login with invalid credentials
        fillInputById("email", "nonexistent@example.com");
        fillInputById("password", "WrongPassword123!");
        
        clickButton("Log In");
        
        // Wait for error message
        page.waitForTimeout(1000);
        
        // Should still be on login page with error
        assertTrue(page.url().contains("/login"), "Should remain on login page after failed login");
    }
}
