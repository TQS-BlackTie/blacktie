package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for renter viewing their booking history
 */
class RenterHistoryE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testViewRenterHistory() {
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
        
        // Should be on role-setup page, select renter role
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
        
        // Navigate to my bookings
        navigateToMyBookings();
        page.waitForTimeout(2000);
        
        // Verify we're on the bookings page
        assertTrue(
            page.url().contains("/my-bookings"),
            "Expected to be on my-bookings page. Current URL: " + page.url()
        );
    }
}
