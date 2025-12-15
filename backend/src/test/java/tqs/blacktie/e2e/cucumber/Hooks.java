package tqs.blacktie.e2e.cucumber;

import com.microsoft.playwright.*;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.BeforeAll;
import io.cucumber.java.AfterAll;

public class Hooks {
    private static Playwright playwright;
    private static Browser browser;
    public static BrowserContext context;
    public static Page page;

    @BeforeAll
    public static void globalSetup() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    public static void globalTeardown() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @Before
    public void beforeScenario() {
        context = browser.newContext();
        page = context.newPage();
    }

    @After
    public void afterScenario() {
        if (context != null) context.close();
    }
}
