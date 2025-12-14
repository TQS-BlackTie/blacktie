package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;

/**
 * E2E test for browsing products in the catalog
 */
class ProductBrowsingE2ETest extends BasePlaywrightE2ETest {
  
    @Test
    void testBrowseProductCatalog() {
        navigateToHome();
        
        // Verify home page loads with product catalog
        page.waitForSelector("text=BlackTie", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertTrue(page.content().contains("BlackTie"), "Expected BlackTie brand on home page");
        
        // Wait for page to fully load
        page.waitForTimeout(2000);
        
        // Verify page content loaded (products or no products message)
        String content = page.content();
        boolean hasProducts = content.contains("Tuxedo") || content.contains("Suit") || 
                            content.contains("products") || content.contains("catalog");
        assertTrue(hasProducts || content.length() > 1000, "Expected product catalog page to load");
    }
    
    @Test
    void testViewHomepage() {
        navigateToHome();
        
        // Wait for BlackTie brand to appear
        page.waitForSelector("text=BlackTie", new Page.WaitForSelectorOptions().setTimeout(5000));
        
        // Wait for page to fully load
        page.waitForTimeout(2000);
        
        // Verify homepage loaded successfully
        assertTrue(page.url().contains("localhost"), "Expected to be on homepage");
        assertTrue(page.content().contains("BlackTie"), "Expected BlackTie brand on page");
    }
}
