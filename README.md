# blacktie

## End-to-end tests (Playwright)

A small, general Playwright smoke test suite lives in `backend/src/test/java/tqs/blacktie/e2e/playwright` and can be run from the `backend` directory. The simplified smoke tests check basic, high-level flows (home page, navigation, register/login, and product catalog) and are suitable for local development.

Quick run:

```bash
# start frontend (optional but recommended)
cd frontend
npm install
npm run dev

# in a new terminal: run the tests (backend folder)
cd ../backend
mvn -Dtest=tqs.blacktie.e2e.playwright.SimplePlaywrightE2ETest test
```

See `backend/src/test/java/tqs/blacktie/e2e/playwright/README.md` for more details and troubleshooting tips.
# blacktie