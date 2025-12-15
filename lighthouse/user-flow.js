import puppeteer from 'puppeteer';
import { startFlow } from 'lighthouse';
import fs from 'fs';

/**
 * BlackTie Lighthouse User Flow
 * 
 * Flow Steps:
 * 1. Navigate to login page (Snapshot: Page loaded)
 * 2. Login with credentials
 * 3. Wait for home page to load (Snapshot: Home page loaded)
 * 4. Search for "blazer"
 * 5. Click on a product (Snapshot: Product details opened)
 * 6. Open booking modal and select dates (23-24 December)
 * 7. Submit booking (Snapshot: Booking submitted)
 */

// Note: Using IP address because Chrome blocks the hostname due to HSTS/private network access
const BASE_URL = process.env.BASE_URL || 'http://192.168.160.34';

// Test user credentials - change these to match your test user
const TEST_USER = {
    email: 'joao@email.pt',
    password: 'renter123'
};

// Helper function to wait (replaces deprecated page.waitForTimeout)
const wait = (ms) => new Promise(resolve => setTimeout(resolve, ms));

async function runUserFlow() {
    console.log('🚀 Starting Lighthouse User Flow for BlackTie...\n');
    console.log(`📍 Base URL: ${BASE_URL}`);
    console.log(`👤 Test User: ${TEST_USER.email}\n`);

    const browser = await puppeteer.launch({
        headless: 'new',  // Use newer headless mode
        ignoreHTTPSErrors: true,
        args: [
            '--no-sandbox',
            '--disable-setuid-sandbox',
            '--disable-web-security',
            '--disable-features=BlockInsecurePrivateNetworkRequests',
            '--allow-running-insecure-content',
            '--disable-blink-features=AutomationControlled',
            '--disable-extensions',
            '--ignore-certificate-errors',
            '--ignore-certificate-errors-spki-list'
        ],
    });

    const page = await browser.newPage();

    // Set a realistic user agent to avoid being blocked
    await page.setUserAgent('Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36');

    // Set viewport for consistent testing
    await page.setViewport({ width: 1280, height: 800 });

    // Start Lighthouse User Flow
    const flow = await startFlow(page, {
        name: 'BlackTie Booking Flow',
        configContext: {
            settingsOverrides: {
                formFactor: 'desktop',
                screenEmulation: { disabled: true },
            },
        },
    });

    try {
        // ==========================================================
        // STEP 1: Navigation - Login Page (with wait for content)
        // ==========================================================
        console.log('📖 Step 1: Navigating to login page...');

        // First, navigate and wait for the page to be fully loaded with content
        await page.goto(`${BASE_URL}/login`, { waitUntil: 'networkidle2', timeout: 30000 });

        // Wait for React to render the form
        console.log('⏳ Waiting for React to render...');
        await wait(2000);

        // Wait for login form to be present
        try {
            await page.waitForSelector('#email', { visible: true, timeout: 10000 });
            console.log('✅ Login form found!');
        } catch (e) {
            console.log('⚠️  Email input not found, trying alternative selector...');
            await page.waitForSelector('input[type="email"]', { visible: true, timeout: 5000 });
        }

        // Now take the Lighthouse snapshot
        console.log('📸 Step 2: Capturing login page snapshot...');
        await flow.snapshot({ stepName: 'Login Page Loaded' });

        // ==========================================================
        // STEP 3: Timespan - Login Process
        // ==========================================================
        console.log('⏱️  Step 3: Performing login...');
        await flow.startTimespan({ stepName: 'User Login' });

        // Fill in login form (already waited for #email above)
        await page.type('#email', TEST_USER.email, { delay: 30 });
        await page.type('#password', TEST_USER.password, { delay: 30 });

        // Click login button
        await page.click('button[type="submit"]');

        // Wait for redirect (may go to /role-setup or /home depending on user)
        console.log('⏳ Waiting for post-login redirect...');
        await page.waitForNavigation({ waitUntil: 'networkidle2', timeout: 20000 }).catch(() => { });
        await wait(2000);

        // Check if we're on role-setup page (intermediate step for new users)
        let currentUrl = page.url();
        console.log(`📍 Current URL after login: ${currentUrl}`);

        if (currentUrl.includes('role-setup')) {
            console.log('📋 On role-setup page, selecting renter role...');
            await wait(2000); // Wait for page to load

            // Click on the "renter" radio option
            try {
                await page.evaluate(() => {
                    const renterLabel = Array.from(document.querySelectorAll('label')).find(
                        l => l.textContent?.toLowerCase().includes('renter') || l.textContent?.toLowerCase().includes('rent')
                    );
                    if (renterLabel) {
                        const radio = renterLabel.querySelector('input[type="radio"]') ||
                            document.querySelector('input[value="renter"]');
                        if (radio) radio.click();
                        renterLabel.click();
                    }
                });
                await wait(500);

                // Click confirm button
                await page.evaluate(() => {
                    const confirmBtn = Array.from(document.querySelectorAll('button')).find(
                        b => b.textContent?.toLowerCase().includes('confirm')
                    );
                    if (confirmBtn) confirmBtn.click();
                });

                await wait(3000); // Wait for redirect after role selection
                currentUrl = page.url();
                console.log(`📍 URL after role selection: ${currentUrl}`);
            } catch (e) {
                console.log('⚠️  Could not complete role-setup, navigating to home directly...');
            }
        }

        // If still not on home page, navigate there
        if (!currentUrl.includes('/home') && !currentUrl.endsWith('/')) {
            console.log('🏠 Navigating to home page...');
            await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle2', timeout: 20000 });
            await wait(2000);
        }

        // Wait for the product catalog to load
        try {
            await page.waitForSelector('#search', { visible: true, timeout: 15000 });
            console.log('✅ Product catalog loaded!');
        } catch (e) {
            console.log('⚠️  Search input not found on current page, trying root...');
            await page.goto(`${BASE_URL}/`, { waitUntil: 'networkidle2', timeout: 20000 });
            await wait(3000);
            await page.waitForSelector('#search', { visible: true, timeout: 15000 });
        }

        await flow.endTimespan();

        // ==========================================================
        // STEP 4: Snapshot - Home Page After Login
        // ==========================================================
        console.log('📸 Step 4: Capturing home page snapshot...');
        await wait(1000); // Brief pause for full render
        await flow.snapshot({ stepName: 'Home Page - Logged In' });

        // ==========================================================
        // STEP 5: Timespan - Search for Blazer
        // ==========================================================
        console.log('🔍 Step 5: Searching for "blazer"...');
        await flow.startTimespan({ stepName: 'Search for Blazer' });

        // Type in the search box
        const searchInput = await page.waitForSelector('#search', { visible: true });
        await searchInput.click({ clickCount: 3 }); // Select all existing text
        await page.type('#search', 'blazer', { delay: 50 });

        // Click the Search button
        await page.click('button[type="submit"]');

        // Wait for search results to load
        await wait(2000);

        await flow.endTimespan();

        // ==========================================================
        // STEP 6: Snapshot - Search Results
        // ==========================================================
        console.log('📸 Step 6: Capturing search results snapshot...');
        await flow.snapshot({ stepName: 'Blazer Search Results' });

        // ==========================================================
        // STEP 7: Timespan - Open Product Detail
        // ==========================================================
        console.log('🎯 Step 7: Opening product detail...');
        await flow.startTimespan({ stepName: 'Open Product Detail' });

        // Click on the first product card
        const productCards = await page.$$('[class*="cursor-pointer"][class*="shadow-lg"]');
        if (productCards.length > 0) {
            await productCards[0].click();
            await wait(1000); // Wait for modal to open
        } else {
            console.log('⚠️  No products found! Make sure there is a "blazer" product.');
        }

        await flow.endTimespan();

        // ==========================================================
        // STEP 8: Snapshot - Product Detail Modal
        // ==========================================================
        console.log('📸 Step 8: Capturing product detail snapshot...');
        await flow.snapshot({ stepName: 'Product Detail Modal' });

        // ==========================================================
        // STEP 9: Timespan - Open Booking Modal and Select Dates
        // ==========================================================
        console.log('📅 Step 9: Opening booking modal and selecting dates (23-24 December)...');
        await flow.startTimespan({ stepName: 'Open Booking Modal & Select Dates' });

        // Click "Reserve" button to open booking modal
        await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const reserveBtn = buttons.find(btn => btn.textContent?.includes('Reserve'));
            if (reserveBtn) reserveBtn.click();
        });

        await wait(1000); // Wait for booking modal to open

        // Navigate to December 2024 if not already there
        // Click next month button until we reach December
        const targetMonth = 'December';
        let maxClicks = 12;
        while (maxClicks > 0) {
            const currentMonthText = await page.evaluate(() => {
                const monthSpan = document.querySelector('.text-sm.font-semibold');
                return monthSpan?.textContent || '';
            });

            if (currentMonthText.includes(targetMonth)) break;

            // Click next month button
            const nextButtons = await page.$$('button[type="button"]');
            for (const btn of nextButtons) {
                const text = await btn.evaluate(el => el.textContent);
                if (text === '›') {
                    await btn.click();
                    await wait(300);
                    break;
                }
            }
            maxClicks--;
        }

        // Click on day 23 (start date)
        await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const day23 = buttons.find(btn => {
                const text = btn.textContent?.trim();
                const isCalendarDay = btn.className.includes('rounded-full');
                return text === '23' && isCalendarDay && !btn.disabled;
            });
            if (day23) day23.click();
        });
        await wait(300);

        // Click on day 24 (end date)
        await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button'));
            const day24 = buttons.find(btn => {
                const text = btn.textContent?.trim();
                const isCalendarDay = btn.className.includes('rounded-full');
                return text === '24' && isCalendarDay && !btn.disabled;
            });
            if (day24) day24.click();
        });
        await wait(500);

        await flow.endTimespan();

        // ==========================================================
        // STEP 10: Snapshot - Dates Selected
        // ==========================================================
        console.log('📸 Step 10: Capturing booking modal with dates snapshot...');
        await flow.snapshot({ stepName: 'Booking Modal - Dates Selected (23-24 Dec)' });

        // ==========================================================
        // STEP 11: Timespan - Submit Booking
        // ==========================================================
        console.log('✅ Step 11: Submitting booking request...');
        await flow.startTimespan({ stepName: 'Submit Booking Request' });

        // Click submit button
        await page.evaluate(() => {
            const buttons = Array.from(document.querySelectorAll('button[type="submit"]'));
            const submitBtn = buttons.find(btn =>
                btn.textContent?.includes('Submit') || btn.textContent?.includes('Booking')
            );
            if (submitBtn) submitBtn.click();
        });

        await wait(2000); // Wait for submission

        await flow.endTimespan();

        // ==========================================================
        // STEP 12: Snapshot - Booking Submitted
        // ==========================================================
        console.log('📸 Step 12: Capturing final snapshot...');
        await flow.snapshot({ stepName: 'Booking Request Submitted' });

    } catch (error) {
        console.error('❌ Error during flow:', error.message);
    }

    // ==========================================================
    // Generate Report
    // ==========================================================
    console.log('\n📊 Generating Lighthouse report...');

    let reportPath = 'lighthouse-user-flow-report.html';
    try {
        const report = await flow.generateReport();
        fs.writeFileSync(reportPath, report);
        console.log(`✅ Report saved to: ${reportPath}`);

        // Also generate JSON for programmatic analysis
        const jsonReport = JSON.stringify(await flow.createFlowResult(), null, 2);
        fs.writeFileSync('lighthouse-user-flow-report.json', jsonReport);
        console.log('✅ JSON report saved to: lighthouse-user-flow-report.json');
    } catch (reportError) {
        console.log('⚠️  Could not generate report:', reportError.message);
    }

    await browser.close();

    console.log('\n🎉 Lighthouse User Flow completed!');
    console.log(`\n📂 Open ${reportPath} in your browser to view the report.`);
}

// Run the flow
runUserFlow().catch(console.error);
