package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;

class LoginPageE2ETest extends BasePlaywrightE2ETest {

    @Test
    void loginPageHasEmailPasswordAndButton() {
        navigateToLogin();

        // Ensure form fields are present
        assertElementVisible("#email");
        assertElementVisible("#password");

        // Ensure the login action is present
        assertTextVisible("Log In");

        takeScreenshot("login-page-basic");
    }
}
