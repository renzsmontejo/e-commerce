# E-commerce

Angular 21 / TypeScript / Angular Material frontend and Spring Boot 4.1 / Java 27 backend, with Spring Security, JPA/Hibernate, MySQL 8, Flyway, Jakarta validation, JUnit/Mockito/Testcontainers, OpenAPI/Swagger UI, local product-image storage, and Docker/Nginx deployment.

## Project layout

- `ecommerce-backend/src/main/java/com/example/ecommerce`: feature packages for auth, user, product, category, cart, order, payment, inventory, review, and report; shared config/security/common packages.
- `ecommerce-backend/src/main/resources/application.yml`: environment-backed configuration.
- `ecommerce-backend/src/main/resources/db/migration`: versioned SQL migrations. The conditional Java migration is in `src/main/java/db/migration`.
- `ecommerce-backend/src/main/resources/db/legacy`: archived manual scripts, outside Flyway's scan path.
- `ecommerce-frontend/src/app`: core auth/guards/interceptors/services, shared components/models/pipes, and feature folders. Authentication forms use Material fields, inputs, errors, hints, and buttons; the account uses a Material card.

## Backend development and tests

Use an installed JDK 27 and keep Docker Desktop running. For PowerShell:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27.0.1.0.2'
cd ecommerce-backend
.\mvnw.cmd clean verify
```

Integration tests start a disposable MySQL 8.0.46 container, run Flyway, and validate the schema. They do not connect to the application database on port 3308. The migration test upgrades a separate disposable legacy schema and checks preserved user/product values. The concurrency tests use committed temporary fixtures inside the test container and clean them up. Docker is required; integration tests are not silently skipped.

To run the backend locally, start MySQL and provide DB_URL, DB_USERNAME, DB_PASSWORD, and JWT_SECRET. Defaults point to `ecommerce_db` on localhost:3308. JWT_SECRET must be a Base64-encoded random key of at least 32 bytes. Generate a development key in the backend shell:

```powershell
$jwtBytes = New-Object byte[] 32
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
.\mvnw.cmd spring-boot:run
```

Preserve the JWT key across deployed restarts. Configure SMTP for password recovery and PayMongo keys for online payments; see the API guides below.

## Flyway migrations

Flyway owns schema changes; Hibernate uses `ddl-auto: validate`. Startup runs V1 (canonical schema, creates missing tables without drops), V2 (conditional compatibility for legacy user/product columns), then V3 (CUSTOMER/ADMIN role seeds, with no default user credentials). Previously applied versions are validated and are not rerun. Add new migrations instead of editing applied ones. Flyway clean is disabled.

Fresh databases migrate automatically. The inspected local database already had an empty Flyway history table and can migrate without a baseline. For another existing database without history, back it up and inspect the schema first. Only for that explicit initial adoption, set FLYWAY_BASELINE_ON_MIGRATE=true (baseline version 0), then remove the override. Automatic baseline is disabled by default to avoid accepting an unintended database. Legacy compatibility preserves existing rows and unused legacy values.

## Frontend development

```powershell
cd ecommerce-frontend
npm ci
npm start
```

Open `http://localhost:4200/auth/login` or `/auth/register`. The Angular development proxy forwards `/api/**` (including Swagger assets) to port 8080. The Material theme is defined in `src/material-theme.scss`; layout styling stays responsive. Reactive Forms and HttpClient remain the form and API layers.

```powershell
npm run build
npm test -- --watch=false
```

The production build is written to `ecommerce-frontend/dist/frontend/browser`. JWTs are held in memory and persisted in tab-scoped sessionStorage to support refresh (JavaScript can access this storage). Startup verifies the token with `/api/auth/me` before restoring authentication; invalid/expired tokens and logout clear it. If browser storage is unavailable, authentication remains memory-only. Reserved feature folders do not add unimplemented routes.

## API documentation

With the backend running:

- Swagger UI: `http://localhost:8080/api/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/api/docs`
- With the Docker frontend: `http://localhost:4200/api/swagger-ui`

Swagger has an Authorize action for bearer JWTs. Public auth/catalog/webhook operations are documented without bearer authentication; the PayMongo signature header is documented separately. Protected APIs still enforce authentication and ADMIN roles. The relative API server URL works through the reverse proxy.

Endpoint guides:

- [Authentication](ecommerce-backend/AUTH_API.md)
- [Products and categories](ecommerce-backend/CATALOG_API.md)
- [Cart and checkout](ecommerce-backend/CART_CHECKOUT_API.md)
- [Orders and PayMongo](ecommerce-backend/ORDERS_PAYMENTS_API.md)

## Docker deployment

The source Dockerfiles build the frontend and backend. For an already packaged backend JAR, use:

```powershell
docker build -f ecommerce-backend/Dockerfile.runtime -t ecommerce-backend:stack ecommerce-backend
docker build -t ecommerce-frontend:material ecommerce-frontend
```

Run the backend on `ecommerce-network` as `ecommerce-backend`, publishing 8080, with the database/JWT/SMTP/PayMongo environment preserved. Mount `ecommerce-product-images` at `/app/uploads/product-images`. Ensure old environment overrides do not set Hibernate back to update/none: use SPRING_JPA_HIBERNATE_DDL_AUTO=validate. Run the frontend as `ecommerce-frontend`, publishing `4200:80`, on the same network. Nginx forwards API and Swagger assets, supports Angular routing, and accepts batch image uploads up to 51 MiB.

After replacing the backend container, reload the frontend proxy to refresh its upstream address:

```powershell
docker exec ecommerce-frontend nginx -s reload
```

Take a database backup before deploying new migrations. Retain image volumes and the JWT key when recreating containers. The local stack uses MySQL 8 on port 3308; the isolated test database uses a dynamically assigned port.
