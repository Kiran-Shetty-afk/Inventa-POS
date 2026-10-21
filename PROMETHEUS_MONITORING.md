# Prometheus Monitoring Setup - Inventa POS

This document provides complete instructions for running and verifying Prometheus monitoring across the Inventa POS microservices architecture.

---

## 1. Overview
Prometheus collects real-time application metrics (JVM performance, HTTP request counts/latencies, database connection pool statistics, CPU/memory usage, and error rates) from each microservice using Spring Boot Actuator and Micrometer.

### Monitored Services & Target Endpoints

| Service Name | Port | Host Target (Docker Bridge) | Actuator Prometheus URL |
| :--- | :--- | :--- | :--- |
| **Prometheus (Self)** | `9090` | `localhost:9090` | `http://localhost:9090/metrics` |
| **Eureka Server** | `8761` | `host.docker.internal:8761` | `http://localhost:8761/actuator/prometheus` |
| **API Gateway** | `5000` | `host.docker.internal:5000` | `http://localhost:5000/actuator/prometheus` |
| **User & Org Service** | `8081` | `host.docker.internal:8081` | `http://localhost:8081/actuator/prometheus` |
| **Inventory Catalog Service** | `8082` | `host.docker.internal:8082` | `http://localhost:8082/actuator/prometheus` |
| **Order & Sales Service** | `8083` | `host.docker.internal:8083` | `http://localhost:8083/actuator/prometheus` |
| **Billing & Analytics Service** | `8084` | `host.docker.internal:8084` | `http://localhost:8084/actuator/prometheus` |

---

## 2. Prerequisites & Architecture
- **Spring Boot 3.4.3** with `spring-boot-starter-actuator` and `micrometer-registry-prometheus` configured in each microservice.
- **Docker Desktop** for running containerized Prometheus.
- `host.docker.internal` is mapped via `extra_hosts` in `docker-compose.yml` so the Prometheus container can reliably scrape microservices running on the host machine across Windows, Mac, and Linux.

---

## 3. How to Start Prometheus

From the project root directory (`C:\Desktop\Inventa-POS`):

```powershell
docker compose up -d
```

To view live container logs:
```powershell
docker logs -f inventa-prometheus
```

---

## 4. How to Verify & Check Targets

1. **Prometheus Web UI**:
   Open: [http://localhost:9090](http://localhost:9090)

2. **Scrape Targets Dashboard**:
   Open: [http://localhost:9090/targets](http://localhost:9090/targets)
   *(All monitored services should show state **UP** after the microservices are started/restarted)*.

3. **Sample PromQL Metrics to Query in Prometheus**:
   - Total HTTP Requests:
     ```promql
     http_server_requests_seconds_count
     ```
   - Request Duration / Latency:
     ```promql
     http_server_requests_seconds_sum / http_server_requests_seconds_count
     ```
   - JVM Memory Usage:
     ```promql
     jvm_memory_used_bytes{area="heap"}
     ```
   - System CPU Usage:
     ```promql
     system_cpu_usage
     ```
   - Active Microservices Health:
     ```promql
     up
     ```

---

## 5. How to Stop Prometheus

To stop the Prometheus container:
```powershell
docker compose down
```

To stop and remove saved metric volumes:
```powershell
docker compose down -v
```

---

## 6. How Another Developer Can Reproduce After Cloning

1. Clone the repository:
   ```bash
   git clone https://github.com/Kiran-Shetty-afk/Inventa-POS.git
   cd Inventa-POS
   ```
2. Start the Spring Boot microservices:
   ```powershell
   .\start-services.ps1
   ```
3. Start Prometheus with Docker Compose:
   ```powershell
   docker compose up -d
   ```
4. Navigate to `http://localhost:9090/targets` to view metrics collection in action.
