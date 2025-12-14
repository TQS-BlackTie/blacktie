/**
 * k6 Load Testing Script for BlackTie Backend
 * ============================================
 * 
 * This script performs load testing against the BlackTie backend API and includes:
 * - SLO thresholds for response time and error rates
 * - Multiple test scenarios (smoke, load, stress)
 * - Prometheus remote write integration
 * - JSON report generation
 * 
 * Usage:
 *   docker exec -it k6-runner k6 run /scripts/test.js
 *   
 * With Prometheus output:
 *   docker exec -it k6-runner k6 run /scripts/test.js -o experimental-prometheus-rw
 */

import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// =============================================================================
// Custom Metrics
// =============================================================================
const errorRate = new Rate('errors');
const apiResponseTime = new Trend('api_response_time', true);
const successfulRequests = new Counter('successful_requests');
const failedRequests = new Counter('failed_requests');

// =============================================================================
// Configuration
// =============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

// =============================================================================
// SLO Thresholds (Service Level Objectives)
// =============================================================================
export const options = {
    // Test scenarios
    scenarios: {
        // Smoke test: Quick validation with minimal load
        smoke_test: {
            executor: 'constant-vus',
            vus: 1,
            duration: '30s',
            startTime: '0s',
            tags: { test_type: 'smoke' },
        },

        // Load test: Normal expected load
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 10 },   // Ramp up to 10 users
                { duration: '2m', target: 10 },   // Stay at 10 users
                { duration: '1m', target: 20 },   // Ramp up to 20 users
                { duration: '2m', target: 20 },   // Stay at 20 users
                { duration: '1m', target: 0 },    // Ramp down to 0
            ],
            startTime: '30s',
            tags: { test_type: 'load' },
        },

        // Stress test: Beyond normal capacity
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 50 },  // Ramp up quickly
                { duration: '1m', target: 50 },   // Stay at peak
                { duration: '30s', target: 0 },   // Ramp down
            ],
            startTime: '8m',
            tags: { test_type: 'stress' },
        },
    },

    // SLO Thresholds - these define pass/fail criteria
    thresholds: {
        // HTTP request duration thresholds
        'http_req_duration': [
            'p(50)<100',    // 50% of requests should be below 100ms
            'p(90)<200',    // 90% of requests should be below 200ms
            'p(95)<300',    // 95% of requests should be below 300ms (SLO requirement)
            'p(99)<500',    // 99% of requests should be below 500ms
        ],

        // Custom API response time thresholds
        'api_response_time': [
            'p(95)<300',    // 95% of API calls under 300ms
        ],

        // Error rate thresholds (SLO: less than 1% failure rate)
        'errors': ['rate<0.01'],

        // HTTP failure rate
        'http_req_failed': ['rate<0.01'],

        // Successful requests should be the majority
        'checks': ['rate>0.99'],
    },
};

// =============================================================================
// Setup Function - Runs once before all VUs start
// =============================================================================
export function setup() {
    console.log('🚀 Starting k6 Load Test for BlackTie Backend');
    console.log(`📍 Target URL: ${BASE_URL}`);

    // Verify the target is reachable
    const healthCheck = http.get(`${BASE_URL}/api/health`, {
        timeout: '10s',
        tags: { name: 'health_check' },
    });

    if (healthCheck.status !== 200) {
        console.warn(`⚠️ Health check returned status ${healthCheck.status}`);
    } else {
        console.log('✅ Health check passed');
    }

    return {
        startTime: new Date().toISOString(),
        baseUrl: BASE_URL,
    };
}

// =============================================================================
// Main Test Function - Runs for each VU iteration
// =============================================================================
export default function (data) {
    // Group: Health Check Endpoint
    group('Health Check', () => {
        const response = http.get(`${BASE_URL}/api/health`, {
            tags: { name: 'GET /api/health' },
        });

        const checkResult = check(response, {
            'health check status is 200': (r) => r.status === 200,
            'health check response time < 100ms': (r) => r.timings.duration < 100,
        });

        apiResponseTime.add(response.timings.duration);

        if (checkResult) {
            successfulRequests.add(1);
            errorRate.add(false);
        } else {
            failedRequests.add(1);
            errorRate.add(true);
        }
    });

    sleep(0.5); // Small pause between groups

    // Group: BlackTie API Endpoint Tests
    group('BlackTie API Endpoints', () => {
        // Test the actual BlackTie API endpoints
        // Note: Some endpoints require X-User-Id header

        // 1. Health endpoint (no auth required)
        const healthResponse = http.get(`${BASE_URL}/api/health`, {
            tags: { name: 'GET /api/health' },
        });

        check(healthResponse, {
            'GET /api/health status is 200': (r) => r.status === 200,
            'GET /api/health response time < 100ms': (r) => r.timings.duration < 100,
            'GET /api/health returns status': (r) => {
                try {
                    const body = JSON.parse(r.body);
                    return body.status === 'Up';
                } catch (e) {
                    return false;
                }
            },
        });
        apiResponseTime.add(healthResponse.timings.duration);

        sleep(0.3);

        // 2. Products endpoint (requires X-User-Id header)
        // Using a test user ID - adjust as needed for your setup
        const productsResponse = http.get(`${BASE_URL}/api/products`, {
            headers: {
                'X-User-Id': '1',  // Test user ID
                'Content-Type': 'application/json',
            },
            tags: { name: 'GET /api/products' },
        });

        const productsCheck = check(productsResponse, {
            'GET /api/products status is 2xx': (r) => r.status >= 200 && r.status < 300,
            'GET /api/products response time < 300ms': (r) => r.timings.duration < 300,
        });
        apiResponseTime.add(productsResponse.timings.duration);

        if (productsCheck) {
            successfulRequests.add(1);
            errorRate.add(false);
        } else {
            failedRequests.add(1);
            errorRate.add(true);
        }

        sleep(0.3);
    });

    sleep(1); // Pause between iterations
}

// =============================================================================
// Teardown Function - Runs once after all VUs complete
// =============================================================================
export function teardown(data) {
    console.log('🏁 Load test completed');
    console.log(`⏱️ Test started at: ${data.startTime}`);
    console.log(`⏱️ Test ended at: ${new Date().toISOString()}`);
}

// =============================================================================
// Custom Summary Handler - Generates Reports
// =============================================================================
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    // Generate a detailed text summary
    let summary = '\n';
    summary += '╔══════════════════════════════════════════════════════════════╗\n';
    summary += '║              K6 LOAD TEST SUMMARY REPORT                     ║\n';
    summary += '╚══════════════════════════════════════════════════════════════╝\n\n';

    // Request Statistics
    if (data.metrics.http_reqs) {
        summary += '📊 REQUEST STATISTICS\n';
        summary += '─────────────────────────────────────────────────────────────────\n';
        summary += `   Total Requests:     ${data.metrics.http_reqs.values.count}\n`;
        summary += `   Request Rate:       ${data.metrics.http_reqs.values.rate.toFixed(2)} req/s\n`;
    }

    // Response Times
    if (data.metrics.http_req_duration) {
        summary += '\n⏱️  RESPONSE TIMES\n';
        summary += '─────────────────────────────────────────────────────────────────\n';
        summary += `   Average:            ${data.metrics.http_req_duration.values.avg.toFixed(2)} ms\n`;
        summary += `   Minimum:            ${data.metrics.http_req_duration.values.min.toFixed(2)} ms\n`;
        summary += `   Maximum:            ${data.metrics.http_req_duration.values.max.toFixed(2)} ms\n`;
        summary += `   P50 (Median):       ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)} ms\n`;
        summary += `   P90:                ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)} ms\n`;
        summary += `   P95:                ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)} ms\n`;
        summary += `   P99:                ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)} ms\n`;
    }

    // Error Rate
    if (data.metrics.http_req_failed) {
        const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
        summary += '\n❌ ERROR RATE\n';
        summary += '─────────────────────────────────────────────────────────────────\n';
        summary += `   Failure Rate:       ${failRate}%\n`;
        summary += `   Failed Requests:    ${data.metrics.http_req_failed.values.passes}\n`;
    }

    // Data Transfer
    if (data.metrics.data_received && data.metrics.data_sent) {
        summary += '\n📦 DATA TRANSFER\n';
        summary += '─────────────────────────────────────────────────────────────────\n';
        summary += `   Data Received:      ${(data.metrics.data_received.values.count / 1024 / 1024).toFixed(2)} MB\n`;
        summary += `   Data Sent:          ${(data.metrics.data_sent.values.count / 1024 / 1024).toFixed(2)} MB\n`;
    }

    // Virtual Users
    if (data.metrics.vus) {
        summary += '\n👥 VIRTUAL USERS\n';
        summary += '─────────────────────────────────────────────────────────────────\n';
        summary += `   Min VUs:            ${data.metrics.vus.values.min}\n`;
        summary += `   Max VUs:            ${data.metrics.vus.values.max}\n`;
    }

    // Threshold Results
    summary += '\n📋 SLO THRESHOLD RESULTS\n';
    summary += '─────────────────────────────────────────────────────────────────\n';

    let thresholdsPassed = true;
    let passedCount = 0;
    let failedCount = 0;

    for (const [metric, metricData] of Object.entries(data.metrics)) {
        if (metricData.thresholds) {
            for (const [name, result] of Object.entries(metricData.thresholds)) {
                if (result.ok) {
                    passedCount++;
                    summary += `   ✅ ${metric}: ${name}\n`;
                } else {
                    failedCount++;
                    thresholdsPassed = false;
                    summary += `   ❌ ${metric}: ${name} (FAILED)\n`;
                }
            }
        }
    }

    summary += '\n═══════════════════════════════════════════════════════════════\n';
    summary += `   THRESHOLDS: ${passedCount} passed, ${failedCount} failed\n`;

    if (thresholdsPassed) {
        summary += '   🎉 ALL SLO THRESHOLDS PASSED!\n';
    } else {
        summary += '   ⚠️  SOME THRESHOLDS FAILED - Review the results above\n';
    }

    summary += '═══════════════════════════════════════════════════════════════\n';

    return {
        // JSON summary for programmatic access
        [`/reports/k6-summary-${timestamp}.json`]: JSON.stringify(data, null, 2),

        // Console output
        stdout: summary,
    };
}
