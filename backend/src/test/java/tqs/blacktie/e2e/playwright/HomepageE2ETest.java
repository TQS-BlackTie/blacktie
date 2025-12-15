package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HomepageE2ETest extends BasePlaywrightE2ETest {

    @Test
    void homepageShowsMainNavigationAndTitle() {
        navigateToHome();

        // Basic perceptive checks: page URL and main navigation items
        assertUrlContains("/");

        // Expect primary actions to be visible (Login / Create Account)
        assertTextVisible("Log In");
        assertTextVisible("Create Account");

        // Take a screenshot for debugging when running locally
        takeScreenshot("homepage-basic");
    }
}
