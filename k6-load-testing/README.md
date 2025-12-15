# from repo root
docker run --rm curlimages/curl:8.4.0 -sS "http://deti-tqs-10.ua.pt/api/health"

cd k6-load-testing

docker run --rm \
  -v "$PWD/scripts":/scripts:ro \
  -v "$PWD/reports":/reports \
  -e BASE_URL="http://deti-tqs-10.ua.pt" \
  -e K6_PROMETHEUS_RW_SERVER_URL="http://host.docker.internal:9090/api/v1/write" \
  --add-host=host.docker.internal:host-gateway \
  -it grafana/k6:0.47.0 \
  run /scripts/test.js -o experimental-prometheus-rw

## 📚 Additional Resources

- [k6 Documentation](https://k6.io/docs/)
- [k6 Prometheus Remote Write](https://k6.io/docs/results-output/real-time/prometheus-remote-write/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Prometheus Documentation](https://prometheus.io/docs/)
