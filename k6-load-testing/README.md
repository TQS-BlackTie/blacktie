# k6 Load Testing Environment

A complete load testing setup using **k6**, **Prometheus**, and **Grafana** for the BlackTie backend application.

## 📁 Directory Structure

```
k6-load-testing/
├── docker-compose.yml           # Infrastructure services
├── prometheus/
│   └── prometheus.yml           # Prometheus configuration
├── grafana/
│   ├── provisioning/
│   │   ├── datasources/
│   │   │   └── datasources.yml  # Prometheus datasource config
│   │   └── dashboards/
│   │       └── dashboards.yml   # Dashboard provisioning config
│   └── dashboards/
│       └── k6-load-testing-dashboard.json  # Pre-configured dashboard
├── scripts/
│   ├── test.js                  # Main load test script
│   └── simple-test.js           # Simple test for quick validation
├── reports/                     # Generated HTML reports (auto-created)
└── README.md                    # This file
```

---

## 🚀 Quick Start

### Part 1: Start Infrastructure

```bash
# Navigate to the k6 testing directory
cd k6-load-testing

# Start all services (Prometheus, Grafana, k6)
docker-compose up -d

# Verify all containers are running
docker-compose ps
```

### Part 2: Run Load Tests

**Option A: Simple Test (Quick Validation)**
```bash
docker exec -it k6-runner k6 run /scripts/simple-test.js -o experimental-prometheus-rw
```

**Option B: Full Test Suite (Smoke, Load, Stress)**
```bash
docker exec -it k6-runner k6 run /scripts/test.js -o experimental-prometheus-rw
```

**Option C: Custom Target URL**
```bash
docker exec -it k6-runner k6 run /scripts/test.js -o experimental-prometheus-rw -e BASE_URL=http://host.docker.internal:8080
```

### Part 3: View Results

1. **Grafana Dashboard**: http://localhost:3000
   - **Username**: `admin`
   - **Password**: `admin`
   - Navigate to **Dashboards** → **k6 Load Testing Dashboard**

2. **Prometheus**: http://localhost:9090
   - Query k6 metrics directly

3. **HTML Reports**: Check `./reports/` directory for generated HTML reports

### Part 4: Cleanup

```bash
# Stop and remove containers
docker-compose down

# Stop and remove containers + volumes (clean slate)
docker-compose down -v
```

---

## 📊 SLO Thresholds

The test scripts include the following Service Level Objectives:

| Metric | Threshold | Description |
|--------|-----------|-------------|
| `http_req_duration` | p(95) < 300ms | 95% of requests must complete under 300ms |
| `http_req_failed` | rate < 0.01 | Less than 1% of requests can fail |
| `checks` | rate > 0.99 | 99% of checks must pass |

---

## 📈 Available Metrics in Grafana

- **HTTP Requests by Status**: Pie chart of successful vs failed requests
- **P95 Response Time**: 95th percentile response time gauge
- **Total HTTP Requests**: Counter of all requests made
- **Response Time Percentiles**: Time series of p50, p90, p95, p99
- **Request Rate (RPS)**: Requests per second over time
- **Virtual Users (VUs)**: Active virtual users during the test
- **Error Rate (%)**: Percentage of failed requests
- **Total Data Received**: Amount of data received from the server

---

## 🔧 Configuration

### Customize Test Target

Edit the `BASE_URL` in the test scripts or pass it as an environment variable:

```bash
docker exec -it k6-runner k6 run /scripts/test.js -e BASE_URL=http://your-server:port
```

### Modify Test Scenarios

Edit `scripts/test.js` to adjust:
- Number of virtual users (VUs)
- Test duration
- Ramp-up/down stages
- Target endpoints

### Add Custom Thresholds

Edit the `thresholds` object in the test options:

```javascript
thresholds: {
  'http_req_duration': ['p(95)<300', 'p(99)<500'],
  'http_req_failed': ['rate<0.01'],
  // Add more thresholds as needed
}
```

---

## 🐛 Troubleshooting

### k6 cannot reach the backend

If testing against your local machine:
- Use `http://host.docker.internal:8080` (Docker Desktop on Mac/Windows)
- On Linux, you may need to use `http://172.17.0.1:8080` or add the `--add-host` flag

### No data in Grafana

1. Verify Prometheus is receiving data: http://localhost:9090/targets
2. Check k6 is running with `-o experimental-prometheus-rw`
3. Ensure the Prometheus datasource is configured correctly in Grafana

### Reports not generating

1. Ensure the `./reports` directory exists and is mounted correctly
2. Check k6 container logs: `docker logs k6-runner`

---

## 📚 Additional Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Prometheus Remote Write](https://k6.io/docs/results-output/real-time/prometheus-remote-write/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
