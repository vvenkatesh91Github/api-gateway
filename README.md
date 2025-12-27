# API Gateway

## Repository Overview
This repository contains the API Gateway for routing and security in a microservices-based application. It handles:

- **Routing** requests from the frontend to downstream services.
- **JWT-based authentication** for protected services.
- **CORS and cookie management** for local development.

---

### Microservices Interaction

| Service        | Domain            | Port | Notes                                                |
| -------------- | ----------------- | ---- | ---------------------------------------------------- |
| React Frontend | app.localhost     | 3000 | Sends requests to API Gateway                        |
| API Gateway    | gateway.localhost | 8080 | Routes requests to Auth/User services, validates JWT |
| Auth Service   | auth.localhost    | 8081 | Handles login/signup, issues JWT cookie              |
| User Service   | users.localhost   | 8082 | Protected service, requires valid JWT                |

---

## Flow

1. **Login Request:**
   - React frontend sends credentials to API Gateway → Auth Service.
   - Auth Service validates credentials and issues JWT in HttpOnly cookie.

2. **Authenticated Requests:**
   - Subsequent requests from React include JWT cookie.
   - API Gateway reads and validates the JWT before forwarding to protected services (e.g., User Service).

3. **Response Handling:**
   - Protected services respond to API Gateway → Gateway forwards response to frontend.

---

## How It Works

- **Routing:** Spring Cloud Gateway routes requests based on URL patterns (`/auth/**`, `/users/**`).
- **JWT Handling:** API Gateway reads JWT from cookies, validates it, and applies authorization filters.
- **CORS & Cookies:** Configured to allow cross-origin requests from `app.localhost:3000` with credentials.
- **HTTPS:** Gateway communicates securely with all microservices using local certificates.

---

## Setting Up Local Development Environment

### Step 0: Challenges Faced

- Cookies not sent due to `HTTP` vs `HTTPS` mismatch.
- CORS issues between frontend and gateway.
- SSL handshake errors in Java (Gateway → Auth/User) because certificates weren’t trusted.
- Browser SSL warnings with self-signed certs.
- Multiple services on `localhost` required unique hostnames.

---

### Step 1: Generate Local Certificates with mkcert

Install mkcert:

```bash
brew install mkcert
mkcert -install
```

Create wildcard certificate:

```bash
mkcert "*.localhost" "localhost" "app.localhost" "gateway.localhost" "auth.localhost" "users.localhost"
```

```bash
mkdir -p ~/certs
mv _wildcard.localhost.pem ~/certs/localhost.pem
mv _wildcard.localhost-key.pem ~/certs/localhost-key.pem
```

Creates:
- `_wildcard.localhost.pem` → certificate
- `_wildcard.localhost-key.pem` → key

Valid for one level deep only (`*.localhost`).
Expires in ~3 years.

---

### Step 2: Convert Certificate to PKCS12 (for Spring Boot)

```bash
openssl pkcs12 -export \
  -in /path/to/_wildcard.localhost+5.pem \
  -inkey /path/to/_wildcard.localhost+5-key.pem \
  -out ~/certs/localhost.p12 \
  -name localhost \
  -password pass:changeit
```

Use this `.p12` in all Spring Boot services.

---

### Step 3: Edit Hosts File

Edit `/etc/hosts`:

```
127.0.0.1 app.localhost
127.0.0.1 gateway.localhost
127.0.0.1 auth.localhost
127.0.0.1 users.localhost
```

Ensures local domains resolve correctly.

---

### Step 4: Import mkcert Root into Java Trust Store

```bash
sudo keytool -import -trustcacerts \
  -alias mkcert-root \
  -file "/Users/<username>/Library/Application Support/mkcert/rootCA.pem" \
  -keystore "$JAVA_HOME/lib/security/cacerts" \
  -storepass changeit -noprompt
```

`$JAVA_HOME` = JDK path used by your services.

Avoids PKIX path building failed SSL errors when Spring Boot calls downstream services.

---

### Step 5: Configure Spring Boot Microservices

**Common SSL Properties**

```properties
server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

**API Gateway (`gateway.localhost:8080`)**

```properties
spring.application.name=api-gateway
server.port=8080
```

**Routes**

```properties
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=https://auth.localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**

spring.cloud.gateway.routes[1].id=user-service
spring.cloud.gateway.routes[1].uri=https://users.localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/users/**
spring.cloud.gateway.routes[1].filters[0]=JwtAuthFilter
```

**Global CORS**

```properties
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins=https://app.localhost:3000
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods=GET,POST,PUT,DELETE,OPTIONS
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders=Authorization,Content-Type,Cookie
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowCredentials=true
```

**Logging**

```properties
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.reactor.netty.http.client=DEBUG
```

**Auth Service (`auth.localhost:8081`)**

```properties
spring.application.name=auth-service
server.port=8081

server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12

jwt.secret=my-super-secret-key-which-is-long-enough-256bits
jwt.expiration=3600000
```

**User Service (`users.localhost:8082`)**

```properties
spring.application.name=user-service
server.port=8082

server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

---

## Step 6: Configure React Frontend for HTTPS

**Install react-scripts HTTPS support:**

In project root:

```env
HTTPS=true
SSL_CRT_FILE=/Users/<username>/certs/localhost.pem
SSL_KEY_FILE=/Users/<username>/certs/localhost-key.pem
HOST=app.localhost
PORT=3000
```

Run at https://app.localhost:3000.

**Fetch Example (with cookies):**

```js
const response = await fetch('https://gateway.localhost:8080/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email, password }),
  credentials: 'include', // important to send cookies
});
```

---

### Step 7: JWT Cookie Flow

- Auth service returns JWT in an HttpOnly cookie.
- API Gateway reads the cookie, validates JWT, and forwards requests to user service.
- React app sends requests with `credentials: 'include'`.

---

### Step 8: Adding New Microservices

- Add new hostname to hosts file, e.g., `new-service.localhost`.
- Include it in mkcert command:

```bash
mkcert "*.localhost" "localhost" "app.localhost" "gateway.localhost" "auth.localhost" "users.localhost" "new-service.localhost"
```

- Convert to `.p12` if it’s a Spring Boot service.
- Update API Gateway routes and CORS as needed.

---

### Step 9: Redis
- For session management or caching, you can run Redis locally using Docker:

```bash
docker run -d \
  --name redis-gateway \
  -p 6380:6379 \
  redis:7
```
- Connect your API Gateway to Redis at `localhost:6380`, check application.yam file for config.


### Step 10: Circuit Breaker Configuration

The API Gateway uses a circuit breaker pattern to improve resilience when calling downstream services (such as the Auth Service). This prevents cascading failures and provides fallback responses if a service is unavailable or slow.

#### How Circuit Breaker Works
- Monitors requests to downstream services.
- If failures or timeouts exceed a threshold, the circuit opens and requests are routed to a fallback controller.
- After a wait period, the circuit allows some requests to test if the service has recovered.

#### Example Configuration (Resilience4j)
Add the following to your `application.yml` to configure the circuit breaker and its timeout:

```yaml
resilience4j:
  circuitbreaker:
    instances:
      authServiceCircuitBreaker:  # Use the name from your code/config
        slidingWindowSize: 10
        minimumNumberOfCalls: 5
        waitDurationInOpenState: 10s
        failureRateThreshold: 50
  timelimiter:
    instances:
      authServiceCircuitBreaker:
        timeoutDuration: 5s  # Increase if your auth service is slow
```

- `timeoutDuration`: How long to wait for a response before considering it a failure (default is 1s).
- `waitDurationInOpenState`: How long the circuit stays open before trying again.
- `failureRateThreshold`: Percentage of failures to open the circuit.

**Note:** Adjust `timeoutDuration` to be longer than your typical downstream service response time to avoid premature fallback.

#### Fallback Controller
When the circuit is open, requests are routed to a fallback controller that returns a default response or error message.

---

### Step 11: Prometheus and Grafana Monitoring Setup

To monitor your microservices, you can use Prometheus and Grafana running in Docker containers.

#### Prometheus Setup
- Prometheus scrapes metrics from your services and stores them for analysis.
- The configuration file is located at `src/main/resources/prometheus.yml`.
- Prometheus runs in Docker and is accessible at [http://localhost:9091](http://localhost:9091).

**Run Prometheus in Docker:**
```bash
docker run -d \
  --name prometheus \
  -p 9091:9090 \
  -v $(pwd)/src/main/resources/prometheus.yml:/etc/prometheus/prometheus.yml \
  prom/prometheus
```
- This maps Prometheus' default port 9090 in the container to 9091 on your host.
- Make sure your services expose `/actuator/prometheus` endpoints for metrics.

#### Grafana Setup
- Grafana is used to visualize metrics collected by Prometheus.
- Grafana runs in Docker and is accessible at [http://localhost:4000](http://localhost:4000).

**Run Grafana in Docker:**
```bash
docker run -d \
  --name grafana \
  -p 4000:3000 \
  grafana/grafana
```
- This maps Grafana's default port 3000 in the container to 4000 on your host.

#### Connecting Grafana to Prometheus
1. Open Grafana at [http://localhost:4000](http://localhost:4000).
2. Login (default: admin/admin).
3. Add Prometheus as a data source:
   - URL: `http://host.docker.internal:9091` (or `http://localhost:9091` if running outside Docker)
4. Import dashboards or create your own to visualize metrics.

---

# Notes

- Make sure all services are using the same mkcert `.p12` for local HTTPS.
- Use `credentials: 'include'` in frontend fetch calls to handle cookies.
- Use `https://` consistently to avoid cookie and CORS issues.
- Java must trust mkcert root to communicate over HTTPS internally.
