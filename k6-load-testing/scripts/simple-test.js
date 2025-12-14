/**
 * Simple k6 Load Test Script for BlackTie Backend
 * ================================================
 * 
 * A minimal test script for quick validation and testing.
 * Tests the /api/health endpoint which doesn't require authentication.
 * 
 * Usage:
 *   docker exec -it k6-runner k6 run /scripts/simple-test.js -o experimental-prometheus-rw
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

// =============================================================================
// Configuration
// =============================================================================
const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';

export const options = {
    // Simple ramping test
    stages: [
        { duration: '30s', target: 5 },   // Ramp up to 5 users
        { duration: '1m', target: 5 },    // Stay at 5 users
        { duration: '30s', target: 0 },   // Ramp down
    ],

    // SLO Thresholds
    thresholds: {
        // 95% of requests should be below 300ms (your SLO requirement)
        'http_req_duration': ['p(95)<300'],

        // Less than 1% of requests should fail (your SLO requirement)
        'http_req_failed': ['rate<0.01'],

        // All checks should pass
        'checks': ['rate>0.99'],
    },
};

// =============================================================================
// Main Test Function
// =============================================================================
export default function () {
    // Test the health endpoint - this exists and doesn't require auth
    const response = http.get(`${BASE_URL}/api/health`, {
        tags: { name: 'GET /api/health' },
    });

    // Validate the response
    check(response, {
        'status is 200': (r) => r.status === 200,
        'response time < 300ms': (r) => r.timings.duration < 300,
        'response contains status': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.status === 'Up';
            } catch (e) {
                return false;
            }
        },
    });

    // Wait between requests (think time)
    sleep(1);
}

// =============================================================================
// Summary Handler - Generates JSON Report
// =============================================================================
export function handleSummary(data) {
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');

    // Generate a simple text summary for console
    let summary = '\n========================================\n';
    summary += '         K6 LOAD TEST SUMMARY\n';
    summary += '========================================\n\n';

    if (data.metrics.http_reqs) {
        summary += `Total Requests: ${data.metrics.http_reqs.values.count}\n`;
        summary += `Request Rate: ${data.metrics.http_reqs.values.rate.toFixed(2)}/s\n`;
    }

    if (data.metrics.http_req_duration) {
        summary += `\nResponse Times:\n`;
        summary += `  Average: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms\n`;
        summary += `  P90: ${data.metrics.http_req_duration.values['p(90)'].toFixed(2)}ms\n`;
        summary += `  P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms\n`;
        summary += `  P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms\n`;
    }

    if (data.metrics.http_req_failed) {
        const failRate = (data.metrics.http_req_failed.values.rate * 100).toFixed(2);
        summary += `\nError Rate: ${failRate}%\n`;
    }

    summary += '\n========================================\n';

    // Check thresholds
    let thresholdsPassed = true;
    for (const [metric, thresholds] of Object.entries(data.metrics)) {
        if (thresholds.thresholds) {
            for (const [name, result] of Object.entries(thresholds.thresholds)) {
                if (!result.ok) {
                    thresholdsPassed = false;
                    summary += `❌ THRESHOLD FAILED: ${metric} - ${name}\n`;
                }
            }
        }
    }

    if (thresholdsPassed) {
        summary += '✅ ALL THRESHOLDS PASSED!\n';
    }

    summary += '========================================\n';

    return {
        // JSON summary for programmatic access
        [`/reports/k6-summary-${timestamp}.json`]: JSON.stringify(data, null, 2),

        // Console output
        stdout: summary,
    };
}
