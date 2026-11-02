# e-commerce

## Backend build

Use JDK 27 and the included Maven wrapper (Maven 3.10.0). Spring Boot
4.1.2 manages the Spring dependencies and Lombok 1.18.48; Connector/J is
updated separately in `ecommerce-backend/pom.xml`.

In PowerShell, select your installed JDK before building:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27.0.1.0.2'
cd ecommerce-backend
.\mvnw.cmd -version
.\mvnw.cmd verify '-Dspring.jpa.hibernate.ddl-auto=none' '-Dlogging.level.org.springframework.security=INFO'
```

In IntelliJ IDEA, select JDK 27 for both the project SDK and the Maven
importer/runner, then reload all Maven projects. Setting `java.version` in
the POM selects the compiler target; it does not select the JDK running Maven.

## JWT signing key

Running the application requires `JWT_SECRET`, a Base64-encoded random
key of at least 32 bytes. For a local PowerShell session, run this from `ecommerce-backend`:

```powershell
$jwtKey = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKey)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKey)
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none --logging.level.org.springframework.security=INFO'
```

For deployments, supply a persistent secret through the environment; changing
the key invalidates existing tokens. The context test generates its own temporary
key and does not require this environment variable.

## Run the authentication UI

Select your installed JDK 27 path above. Start MySQL first; the backend reads
`DB_URL`, `DB_USERNAME` and `DB_PASSWORD`, with local defaults in
`ecommerce-backend/src/main/resources/application.yml`. The default database is
`ecommerce_db` on localhost port 3308. Full backend verification includes a
context test that requires access to this database. The commands above disable
automatic schema updates.

For legacy user tables matching the inspected schema, apply
`ecommerce-backend/src/main/resources/db/migration/001_registration_user_columns.sql` explicitly before registration.
It enables generated user IDs and allows NULL in the unused legacy password
column while retaining `password_hash`. The current local database is already
migrated. This script is not an initial schema or an automatic migration runner.

With the backend running on port 8080, open a second PowerShell terminal:

```powershell
cd ecommerce-frontend
npm ci
npm start
```

Open `http://127.0.0.1:4200/auth/login` or `/auth/register`. The development proxy
forwards `/api/**` to Spring Boot. Registration creates a CUSTOMER account and
offers sign-in. Successful login opens `/account`; ADMIN roles are confirmed by
the server, and `/api/admin/**` requires ADMIN.

## Frontend checks

```powershell
cd ecommerce-frontend
npm run build
npm test -- --watch=false
```

The production frontend is written to `ecommerce-frontend/dist/frontend/browser`; Maven
verification packages the backend at `ecommerce-backend/target/e-commerce-0.0.1-SNAPSHOT.jar`.
The existing Nginx configuration expects a backend host named `ecommerce-backend`
on port 8080. No lint task or automated browser-test runner is configured.

Sessions use an in-memory JWT and end on page refresh. Client logout clears that
state; issued tokens expire after 15 minutes and are not revoked on logout.
Password recovery, social login, persistent sign-in and refresh restoration are
not implemented. The storefront and admin product-management UI remain future work.

See `ecommerce-frontend/AUTH_ARCHITECTURE.md` and the batch reports under `ecommerce-frontend/review/`
for architecture, file inventories, screenshots and validation evidence.

## Rebuild the local Docker backend after API changes

The backend container must be rebuilt after Java changes. A container built from
older code can redirect `/api/auth/*` requests to Spring's default `/login` page.

After packaging with JDK 27 (`mvn package -DskipTests` from `backend`), build:

```powershell
docker build -f ecommerce-backend/Dockerfile.runtime -t ecommerce-backend:auth-fix ecommerce-backend
```

Run this image on `ecommerce-network` with the name `ecommerce-backend`, port
`8080:8080`, the existing `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and a random
Base64 `JWT_SECRET` of at least 32 bytes. Preserve that key across restarts.
Use `SPRING_JPA_HIBERNATE_DDL_AUTO=none` for the existing migrated database.
The runtime Dockerfile uses Java 27 and copies the packaged JAR. Its separate
ignore file includes only that JAR in the build context.

After replacing the backend container, reload Nginx so it resolves the new
container address:

```powershell
docker exec ecommerce-frontend nginx -s reload
```

Verify through the frontend at `http://localhost:4200/api/auth/login`: an empty
JSON POST should return 400, with no redirect or `WWW-Authenticate: Basic` header.
An unauthenticated GET to `/api/auth/me` should return 401.

## Products and categories

See [ecommerce-backend/CATALOG_API.md](ecommerce-backend/CATALOG_API.md) for the public catalog,
ADMIN product management, filter/pagination parameters, multipart image uploads,
and the legacy product compatibility migration. The local catalog image is
`ecommerce-backend:catalog`. Mount `ecommerce-product-images` at
`/app/uploads/product-images` when replacing the backend, and retain the current
JWT/database settings. The frontend Nginx configuration includes the 51 MiB
request limit needed for batch image uploads.


## Project structure

The applications live in `ecommerce-backend/` and `ecommerce-frontend/`.

The backend root package is `com.example.ecommerce`. Each business feature owns
its `model/` and `repository/` packages plus any implemented `controller/`,
`service/`, and `dto/` packages. Features are `auth`, `user`, `product`, `category`,
`cart`, `order`, `payment`, `inventory`, `review`, and `report`. Shared error
handling and enums live in `common`; application/security configuration lives in
`config` and JWT support in `security`. Product image storage lives in
`product/storage`. Category listing has its own controller and service.

Spring configuration is in `ecommerce-backend/src/main/resources/application.yml`.
The existing compatibility SQL scripts now live in `src/main/resources/db/migration`
inside that project. They remain explicit legacy-schema migrations; moving them
has not enabled automatic Flyway execution or changed their application history.

Angular keeps authentication state in `core/auth`, route guards in `core/guards`,
HTTP interceptors in `core/interceptors`, and API configuration in `core/services`.
Reusable layout components and data models live in `shared/components` and
`shared/models`. Authentication pages remain in `features/auth` alongside the
requested feature folders. Empty feature and pipe folders are reserved with
`.gitkeep` files; they do not add routes or unfinished UI.


## Cart and checkout

See [ecommerce-backend/CART_CHECKOUT_API.md](ecommerce-backend/CART_CHECKOUT_API.md)
for the authenticated cart endpoints, checkout preview, and transactional order
placement. Cart code lives in `cart/`; checkout and order code live in `order/`.
Configure `CHECKOUT_SHIPPING_FEE` and `CHECKOUT_TAX_RATE` for store-specific pricing.


## Order lifecycle and payment integration

See [ecommerce-backend/ORDERS_PAYMENTS_API.md](ecommerce-backend/ORDERS_PAYMENTS_API.md)
for history, cancellation, admin status transitions, and the payment integration
contract. Order lifecycle and PayMongo Checkout v2 are implemented. Set `PAYMONGO_SECRET_KEY`
and `PAYMONGO_WEBHOOK_SECRET` on the backend; payment routes return 503 while keys
are absent. Configure a public HTTPS webhook endpoint in the PayMongo dashboard.
