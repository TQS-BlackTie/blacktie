package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for user registration flow
 */
class UserRegistrationE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testUserRegistration() {
        navigateToHome();
        
        // Navigate to signup page
        page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign up")).click();
        
        // Wait for signup form
        page.waitForSelector("#name", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Generate unique user details
        String uniqueName = uniqueName();
        String uniqueEmail = uniqueEmail();
        String password = "TestPassword123!";
        
        // Fill in registration form
        fillInputById("name", uniqueName);
        fillInputById("email", uniqueEmail);
        fillInputById("password", password);
        fillInputById("confirmPassword", password);
        
        // Submit registration
        clickButton("Create Account");
        
        // Wait for success or navigation
        page.waitForTimeout(2000);
        
        // Verify success (either success message or redirect to role setup)
        assertTrue(
            page.content().contains("successfully") || page.url().contains("/role-setup"),
            "Expected successful registration"
        );
    }
    
    @Test
    void testRegistrationValidation() {
        navigateToSignup();
        
        // Try to submit empty form
        clickButton("Create Account");
        
        // Verify validation (HTML5 validation should prevent submission)
        assertTrue(page.url().contains("/signup"), "Should remain on signup page with empty form");
    }
}
