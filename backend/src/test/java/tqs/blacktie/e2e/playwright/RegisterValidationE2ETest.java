package tqs.blacktie.e2e.playwright;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterValidationE2ETest extends BasePlaywrightE2ETest {

    @Test
    void signupFormShowsInvalidEmailAndStaysOnPageWhenInvalid() {
        navigateToSignup();

        // Fill an invalid email and check native HTML5 validation
        fillInputById("email", "not-an-email");
        Object emailValid = page.evaluate("() => document.getElementById('email').checkValidity()");
        assertThat(emailValid).isEqualTo(false);

        // Now fill mismatching passwords and attempt submit; remain on signup page
        String email = uniqueEmail();
        fillInputById("email", email);
        fillInputById("name", uniqueName());
        fillInputById("password", "short");
        fillInputById("confirmPassword", "different");

        clickButton("Create Account");
        waitMs(1000);

        // We expect to still be on /signup when client-side validation prevents submission
        assertUrlContains("/signup");

        takeScreenshot("signup-validation");
    }
}
