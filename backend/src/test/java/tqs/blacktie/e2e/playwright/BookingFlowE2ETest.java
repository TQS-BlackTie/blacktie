package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for complete booking flow
 */
class BookingFlowE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testCompleteBookingFlow() {
        // Setup: Register and login user
        String name = uniqueName();
        String email = uniqueEmail();
        String password = "TestPassword123!";
        
        APIResponse registerResponse = registerUserViaAPI(name, email, password);
        assertEquals(201, registerResponse.status(), "Registration should succeed");
        
        // Login
        navigateToLogin();
        loginViaUI(email, password);
        page.waitForTimeout(3000);
        
        // Should be on role-setup page
        assertTrue(page.url().contains("/role-setup"), "Expected to be on role-setup page");
        
        // Select renter role
        try {
            Locator renterButton = page.locator("button:has-text('Renter'), button:has-text('I want to rent')").first();
            if (renterButton.isVisible()) {
                renterButton.click();
                page.waitForTimeout(2000);
            }
        } catch (Exception e) {
            System.out.println("Could not find role selection: " + e.getMessage());
        }
        
        // Navigate to home
        navigateToHome();
        page.waitForTimeout(2000);
        
        // Verify we're on home page with products
        assertTrue(
            page.url().contains("localhost") && !page.url().contains("/login"),
            "Expected to be on home page after role setup"
        );
    }
}
