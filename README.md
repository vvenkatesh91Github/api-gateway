## API Gateway

### Repository Overview
This repository contains the API Gateway for routing and security in a microservices-based application. It handles:

- Routing requests from the frontend to downstream services.

- JWT-based authentication for protected services.

- CORS and cookie management for local development.

Microservices Interaction

| Service        | Domain            | Port | Notes                                                |
| -------------- | ----------------- | ---- | ---------------------------------------------------- |
| React Frontend | app.localhost     | 3000 | Sends requests to API Gateway                        |
| API Gateway    | gateway.localhost | 8080 | Routes requests to Auth/User services, validates JWT |
| Auth Service   | auth.localhost    | 8081 | Handles login/signup, issues JWT cookie              |
| User Service   | users.localhost   | 8082 | Protected service, requires valid JWT                |

### Flow
1. Login Request:
   - React frontend sends credentials to API Gateway → Auth Service.
   - Auth Service validates credentials and issues JWT in HttpOnly cookie.

2. Authenticated Requests:
   - Subsequent requests from React include JWT cookie.
   - API Gateway reads and validates the JWT before forwarding to protected services (e.g., User Service).

3. Response Handling:
   - Protected services respond to API Gateway → Gateway forwards response to frontend.

### How It Works
- Routing: Spring Cloud Gateway routes requests based on URL patterns (/auth/**, /users/**).
- JWT Handling: API Gateway reads JWT from cookies, validates it, and applies authorization filters.
- CORS & Cookies: Configured to allow cross-origin requests from app.localhost:3000 with credentials.
- HTTPS: Gateway communicates securely with all microservices using local certificates.

-----------------------------

### Setting Up Local Development Environment


#### Step 0: Challenges Faced

- Cookies not sent due to `HTTP` vs `HTTPS` mismatch.
- CORS issues between frontend and gateway.
- SSL handshake errors in Java (Gateway → Auth/User) because certificates weren’t trusted.
- Browser SSL warnings with self-signed certs.
- Multiple services on `localhost` required unique hostnames.

---

#### Step 1: Generate Local Certificates with mkcert

Install mkcert:

```bash```
brew install mkcert
mkcert -install


Create wildcard certificate:

```mkcert "*.localhost" "localhost" "app.localhost" "gateway.localhost" "auth.localhost" "users.localhost"```

```mkdir -p ~/certs
mv _wildcard.localhost.pem ~/certs/localhost.pem
mv _wildcard.localhost-key.pem ~/certs/localhost-key.pem
```

Creates:

_wildcard.localhost.pem → certificate

_wildcard.localhost-key.pem → key

Valid for one level deep only (*.localhost).

Expires in ~3 years.

#### Step 2: Convert Certificate to PKCS12 (for Spring Boot)
```
openssl pkcs12 -export \
-in /path/to/_wildcard.localhost+5.pem \
-inkey /path/to/_wildcard.localhost+5-key.pem \
-out ~/certs/localhost.p12 \
-name localhost \
-password pass:changeit
```

Use this .p12 in all Spring Boot services.

#### Step 3: Edit Hosts File

Edit `/etc/hosts`:

127.0.0.1 app.localhost
127.0.0.1 gateway.localhost
127.0.0.1 auth.localhost
127.0.0.1 users.localhost

Ensures local domains resolve correctly.

#### Step 4: Import mkcert Root into Java Trust Store
```
sudo keytool -import -trustcacerts \
-alias mkcert-root \
-file "/Users/<username>/Library/Application Support/mkcert/rootCA.pem" \
-keystore "$JAVA_HOME/lib/security/cacerts" \
-storepass changeit -noprompt
```


`$JAVA_HOME` = JDK path used by your services.

Avoids PKIX path building failed SSL errors when Spring Boot calls downstream services.

#### Step 5: Configure Spring Boot Microservices
Common SSL Properties
```
server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

API Gateway (`gateway.localhost:8080`)
```
spring.application.name=api-gateway
server.port=8080
```

### Routes
```
spring.cloud.gateway.routes[0].id=auth-service
spring.cloud.gateway.routes[0].uri=https://auth.localhost:8081
spring.cloud.gateway.routes[0].predicates[0]=Path=/auth/**

spring.cloud.gateway.routes[1].id=user-service
spring.cloud.gateway.routes[1].uri=https://users.localhost:8082
spring.cloud.gateway.routes[1].predicates[0]=Path=/users/**
spring.cloud.gateway.routes[1].filters[0]=JwtAuthFilter
```

### Global CORS
```
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedOrigins=https://app.localhost:3000
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedMethods=GET,POST,PUT,DELETE,OPTIONS
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowedHeaders=Authorization,Content-Type,Cookie
spring.cloud.gateway.globalcors.corsConfigurations.[/**].allowCredentials=true
```

### Logging
```
logging.level.org.springframework.cloud.gateway=DEBUG
logging.level.reactor.netty.http.client=DEBUG
```

Auth Service (`auth.localhost:8081`)
```
spring.application.name=auth-service
server.port=8081

server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12

jwt.secret=my-super-secret-key-which-is-long-enough-256bits
jwt.expiration=3600000
```

User Service (`users.localhost:8082`)
```
spring.application.name=user-service
server.port=8082

server.ssl.enabled=true
server.ssl.key-store=/Users/<username>/certs/localhost.p12
server.ssl.key-store-password=changeit
server.ssl.key-store-type=PKCS12
```

## Step 6: Configure React Frontend for HTTPS


### Install react-scripts HTTPS support:

### in project root
```
HTTPS=true
SSL_CRT_FILE=/Users/<username>/certs/localhost.pem
SSL_KEY_FILE=/Users/<username>/certs/localhost-key.pem
HOST=app.localhost
PORT=3000
```


Run at https://app.localhost:3000.

Fetch Example (with cookies)
```
const response = await fetch('https://gateway.localhost:8080/auth/login', {
method: 'POST',
headers: { 'Content-Type': 'application/json' },
body: JSON.stringify({ email, password }),
credentials: 'include', // important to send cookies
});
```

#### Step 7: JWT Cookie Flow

Auth service returns JWT in an HttpOnly cookie.

API Gateway reads the cookie, validates JWT, and forwards requests to user service.

React app sends requests with credentials: 'include'.

####Step 8: Adding New Microservices

Add new hostname to hosts file, e.g., new-service.localhost.

Include it in mkcert command:

```mkcert "*.localhost" "localhost" "app.localhost" "gateway.localhost" "auth.localhost" "users.localhost" "new-service.localhost"```


Convert to .p12 if it’s a Spring Boot service.

Update API Gateway routes and CORS as needed.

# Notes

Make sure all services are using the same mkcert .p12 for local HTTPS.

Use credentials: 'include' in frontend fetch calls to handle cookies.

Use https:// consistently to avoid cookie and CORS issues.

Java must trust mkcert root to communicate over HTTPS internally.