# BlackTie Lighthouse User Flow

This directory contains Lighthouse User Flow tests for the BlackTie application.

## What is Lighthouse User Flow?

Lighthouse User Flow allows you to capture performance, accessibility, SEO, and best practices metrics at multiple points during a user journey, not just on initial page load.

## Flow Steps

The user flow tests the following journey:

1. **Navigate to Login Page** - Full navigation audit
2. **Login Page Loaded** - Snapshot of login form
3. **User Login** - Timespan measuring login interaction
4. **Home Page - Logged In** - Snapshot after successful login
5. **Search for Blazer** - Timespan measuring search interaction
6. **Blazer Search Results** - Snapshot of search results
7. **Open Product Detail** - Timespan measuring product click
8. **Product Detail Modal** - Snapshot of product details
9. **Open Booking Modal & Select Dates** - Timespan measuring date selection
10. **Booking Modal - Dates Selected** - Snapshot with 23-24 December selected
11. **Submit Booking Request** - Timespan measuring booking submission
12. **Booking Request Submitted** - Final snapshot

## Prerequisites

1. **Node.js** (v18 or higher recommended)
2. **Backend running** on `http://localhost:8080`
3. **Frontend running** on `http://localhost:5173` (or configure BASE_URL)
4. **Test user** exists with email `joao@email.pt` and password `renter123`
5. **A "blazer" product** exists in the database

## Installation

```bash
cd blacktie/lighthouse
npm install
```

## Running the Flow

### Option 1: With defaults (localhost:5173)

```bash
npm run flow
```

### Option 2: Custom URL

```bash
BASE_URL=http://your-custom-url npm run flow
```

## Output

After running, you'll get:

- `lighthouse-user-flow-report.html` - Visual HTML report (open in browser)
- `lighthouse-user-flow-report.json` - JSON data for programmatic analysis

## Understanding the Report

The report shows:

| Metric | Description |
|--------|-------------|
| **Performance** | Page speed, rendering performance |
| **Accessibility** | A11y compliance score |
| **Best Practices** | Security, modern APIs, console errors |
| **SEO** | Search engine optimization |

Each step shows:
- For **Navigate**: Full page load metrics
- For **Snapshot**: Current page state audit (no navigation metrics)
- For **Timespan**: Interaction performance over time

## Customization

Edit `user-flow.js` to:

- Change test user credentials (lines 21-24)
- Modify the base URL (line 19)
- Add/remove flow steps
- Change the booking dates

## Troubleshooting

### "No products found"
Make sure there's a product with "blazer" in its name.

### "Login failed"
Verify the test user credentials in `SAMPLE_DATA_REFERENCE.txt`.

### "Timeout waiting for selector"
The frontend may be loading slowly. Increase timeout values in the script.

### "Navigation timeout"
Ensure both backend and frontend are running and accessible.
