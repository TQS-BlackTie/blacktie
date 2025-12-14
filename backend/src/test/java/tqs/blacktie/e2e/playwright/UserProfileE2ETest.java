package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for user profile management
 */
class UserProfileE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testViewUserProfile() {
        // Setup: Register and login user
        String name = uniqueName();
        String email = uniqueEmail();
        String password = "TestPassword123!";
        
        APIResponse registerResponse = registerUserViaAPI(name, email, password);
        assertEquals(201, registerResponse.status(), "Registration should succeed");
        
        // Login via UI
        navigateToLogin();
        loginViaUI(email, password);
        page.waitForTimeout(3000);
        
        // Should be on role-setup page, select a role
        if (page.url().contains("/role-setup")) {
            try {
                Locator renterButton = page.locator("button:has-text('Renter'), button:has-text('I want to rent')").first();
                if (renterButton.isVisible()) {
                    renterButton.click();
                    page.waitForTimeout(2000);
                }
            } catch (Exception e) {
                System.out.println("Role selection not found: " + e.getMessage());
            }
        }
        
        // Navigate to profile
        navigateToProfile();
        page.waitForTimeout(2000);
        
        // Verify profile page loaded
        assertTrue(
            page.url().contains("/profile"),
            "Expected to be on profile page. Current URL: " + page.url()
        );
    }
}
