# BlackTie

## a) Project Abstract

**BlackTie** is a formal event clothing rental platform that connects renters (individuals attending formal events) with owners (businesses offering premium formal wear). The platform enables users to discover, book, and rent high-quality formal attire including tuxedos, evening gowns, accessories, and complete formal ensembles for weddings, galas, corporate events, and other special occasions.

### Key Features

- **Multi-role System**: Support for renters, owners, and administrators with role-specific dashboards
- **Booking Management**: Complete booking workflow from request to approval, payment, and completion
- **Payment Integration**: Secure payment processing with Stripe for deposits and rentals
- **Reputation System**: Review and rating mechanism for quality assurance
- **Notification System**: Real-time updates for booking status changes and important events
- **Admin Dashboard**: User management, analytics, and platform monitoring

### Technology Stack

**Backend:**
- Java 21 + Spring Boot 3.4.0
- Spring Data JPA with PostgreSQL (production) / H2 (testing)
- Spring Security with JWT authentication
- RESTful API architecture
- Stripe payment integration
- WebClient for external API integration

**Frontend:**
- React 19 + TypeScript
- Vite build tool
- Tailwind CSS for styling
- React Router for navigation
- Stripe React components

**Testing:**
- JUnit 5 + Mockito for unit testing
- Cucumber for BDD integration tests
- Playwright for E2E testing
- k6 for load testing
- Lighthouse for performance auditing
- SonarCloud for code quality analysis

**DevOps:**
- Docker & Docker Compose for containerization
- Prometheus & Grafana for monitoring
- Nginx reverse proxy
- CI/CD with GitHub Actions

---

## b) Testing


### Run All Tests

```bash
cd backend
./mvnw test
```

### Run Specific Test Suites

**Unit & Integration Tests:**
```bash
./mvnw test -Dtest='!*E2ETest'
```

**E2E Tests (Playwright):**
```bash
# Ensure frontend is running first
cd frontend && npm run dev

# In another terminal
cd backend
./mvnw test -Dtest='*E2ETest'
```

**Cucumber BDD Tests:**
```bash
./mvnw test -Dtest='*CucumberTest'
```

**Load Testing (k6):**
```bash
cd k6-load-testing
docker compose up
docker exec -it k6-runner k6 run /scripts/test.js
```

---

## c) End-to-End Tests (Playwright)

A comprehensive Playwright E2E test suite is available in `backend/src/test/java/tqs/blacktie/e2e/playwright`. These tests verify critical user flows including authentication, navigation, product catalog, and admin functionality.

**Quick Run:**

```bash
# Start frontend (required)
cd frontend
npm install
npm run dev

# In a new terminal: run the tests
cd backend
./mvnw test -Dtest=tqs.blacktie.e2e.playwright.SimplePlaywrightE2ETest

---

http://deti-tqs-10.ua.pt

**Admin Credentials:**
- Email: `admin@blacktie.pt`
- Password: `admin`
