package tqs.blacktie.e2e.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.RequestOptions;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for Playwright E2E tests.
 * Provides common setup, teardown, and utility methods for browser-based testing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BasePlaywrightE2ETest {
// ... (rest of the file until API Helpers)

    // ==================== API Helpers ====================

    /**
     * Makes a direct API call to register a user (bypasses UI).
     */
    protected APIResponse registerUserViaAPI(String name, String email, String password) {
        APIRequestContext request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(backendUrl("")));
        
        return request.post("/api/auth/register", RequestOptions.create()
                .setData(new java.util.HashMap<String, String>() {{
                    put("name", name);
                    put("email", email);
                    put("password", password);
                }}));
    }

    /**
     * Makes a direct API call to login (bypasses UI).
     */
    protected APIResponse loginViaAPI(String email, String password) {
        APIRequestContext request = playwright.request().newContext(new APIRequest.NewContextOptions()
                .setBaseURL(backendUrl("")));
        
        return request.post("/api/auth/login", RequestOptions.create()
                .setData(new java.util.HashMap<String, String>() {{
                    put("email", email);
                    put("password", password);
                }}));
    }

    @LocalServerPort
    protected int backendPort;

    protected static Playwright playwright;
    protected static Browser browser;
    protected BrowserContext context;
    protected Page page;

    // Default timeouts
    protected static final int DEFAULT_TIMEOUT = 30000;
    protected static final int SHORT_TIMEOUT = 5000;
    protected static final int NAVIGATION_TIMEOUT = 60000;

    // Frontend port (Vite default)
    protected static final int FRONTEND_PORT = 5173;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true) // Run headless for CI/CD
                .setSlowMo(50)); // Slow down operations for visibility
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {
        context = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1920, 1080)
                .setLocale("en-US"));
        context.setDefaultTimeout(DEFAULT_TIMEOUT);
        context.setDefaultNavigationTimeout(NAVIGATION_TIMEOUT);
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) {
            context.close();
        }
    }

    // ==================== URL Helpers ====================

    protected String frontendUrl(String path) {
        return "http://localhost:" + FRONTEND_PORT + path;
    }

    protected String backendUrl(String path) {
        return "http://localhost:" + backendPort + path;
    }

    // ==================== Navigation Helpers ====================

    protected void navigateToHome() {
        page.navigate(frontendUrl("/"));
        waitForPageLoad();
    }

    protected void navigateToLogin() {
        page.navigate(frontendUrl("/login"));
        waitForPageLoad();
    }

    protected void navigateToSignup() {
        page.navigate(frontendUrl("/signup"));
        waitForPageLoad();
    }

    protected void navigateToProfile() {
        page.navigate(frontendUrl("/profile"));
        waitForPageLoad();
    }

    protected void navigateToMyBookings() {
        page.navigate(frontendUrl("/my-bookings"));
        waitForPageLoad();
    }

    protected void navigateToOwnerBookings() {
        page.navigate(frontendUrl("/owner-bookings"));
        waitForPageLoad();
    }

    protected void navigateToAdmin() {
        page.navigate(frontendUrl("/admin"));
        waitForPageLoad();
    }

    // ==================== Wait Helpers ====================

    protected void waitForPageLoad() {
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    protected void waitForElement(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions().setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForElementVisible(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForElementHidden(String selector) {
        page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                .setState(WaitForSelectorState.HIDDEN)
                .setTimeout(DEFAULT_TIMEOUT));
    }

    protected void waitForNavigation(Runnable action) {
        page.waitForNavigation(() -> action.run());
    }

    protected void waitForNetworkIdle() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    protected void waitMs(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== Form Helpers ====================

    protected void fillInput(String selector, String value) {
        page.fill(selector, value);
    }

    protected void fillInputById(String id, String value) {
        page.fill("#" + id, value);
    }

    protected void clickButton(String text) {
        page.click("button:has-text(\"" + text + "\")");
    }

    protected void clickButtonBySelector(String selector) {
        page.click(selector);
    }

    protected void clickLink(String text) {
        page.click("a:has-text(\"" + text + "\")");
    }

    protected void submitForm(String formSelector) {
        page.locator(formSelector).locator("button[type='submit']").click();
    }

    // ==================== Assertion Helpers ====================

    protected void assertUrlContains(String path) {
        assertThat(page.url()).contains(path);
    }

    protected void assertUrlEquals(String expectedUrl) {
        assertThat(page.url()).isEqualTo(expectedUrl);
    }

    protected void assertTextVisible(String text) {
        waitForElementVisible("text=" + text);
        assertThat(page.locator("text=" + text).isVisible()).isTrue();
    }

    protected void assertElementVisible(String selector) {
        waitForElementVisible(selector);
        assertThat(page.locator(selector).isVisible()).isTrue();
    }

    protected void assertElementHidden(String selector) {
        assertThat(page.locator(selector).isHidden()).isTrue();
    }

    protected void assertElementContainsText(String selector, String text) {
        assertThat(page.locator(selector).textContent()).contains(text);
    }

    protected void assertInputValue(String selector, String expectedValue) {
        assertThat(page.inputValue(selector)).isEqualTo(expectedValue);
    }

    // ==================== User Helpers ====================

    protected String uniqueEmail() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com";
    }

    protected String uniqueName() {
        return "User " + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Registers a new user through the UI.
     * @param name User's name
     * @param email User's email
     * @param password User's password
     */
    protected void registerUserViaUI(String name, String email, String password) {
        navigateToSignup();
        waitForElement("#name");
        
        fillInputById("name", name);
        fillInputById("email", email);
        fillInputById("password", password);
        fillInputById("confirmPassword", password);
        
        clickButton("Create Account");
        
        // Wait for success message or navigation
        waitMs(2000);
    }

    /**
     * Logs in a user through the UI.
     * @param email User's email
     * @param password User's password
     */
    protected void loginViaUI(String email, String password) {
        navigateToLogin();
        waitForElement("#email");
        
        fillInputById("email", email);
        fillInputById("password", password);
        
        clickButton("Log In");
        
        // Wait for navigation/success
        waitMs(2000);
    }

    /**
     * Logs out the current user (clears localStorage).
     */
    protected void logout() {
        page.evaluate("() => { localStorage.clear(); }");
        navigateToHome();
    }

    /**
     * Sets user data in localStorage (for quick test setup).
     */
    protected void setUserInLocalStorage(long userId, String name, String email, String role) {
        String script = String.format(
            "() => { " +
            "localStorage.setItem('userId', '%d'); " +
            "localStorage.setItem('user', JSON.stringify({ id: %d, name: '%s', email: '%s', role: '%s' })); " +
            "}",
            userId, userId, name, email, role
        );
        page.evaluate(script);
    }



    // ==================== Screenshot Helpers ====================

    protected void takeScreenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions()
                .setPath(java.nio.file.Paths.get("target/screenshots/" + name + ".png"))
                .setFullPage(true));
    }

    // ==================== Element Locators ====================

    protected Locator getByTestId(String testId) {
        return page.locator("[data-testid='" + testId + "']");
    }

    protected Locator getByRole(String role, String name) {
        return page.getByRole(AriaRole.valueOf(role.toUpperCase()), 
                new Page.GetByRoleOptions().setName(name));
    }

    protected Locator getByText(String text) {
        return page.getByText(text);
    }

    protected Locator getByPlaceholder(String placeholder) {
        return page.getByPlaceholder(placeholder);
    }
}
