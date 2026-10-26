# e-commerce

## Backend build

Use JDK 27 and the included Maven wrapper (Maven 3.10.0). Spring Boot
4.1.2 manages the Spring dependencies and Lombok 1.18.48; Connector/J is
updated separately in `backend/pom.xml`.

In PowerShell, select your installed JDK before building:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-27.0.1.0.2'
cd backend
.\mvnw.cmd -version
.\mvnw.cmd verify '-Dspring.jpa.hibernate.ddl-auto=none' '-Dlogging.level.org.springframework.security=INFO'
```

In IntelliJ IDEA, select JDK 27 for both the project SDK and the Maven
importer/runner, then reload all Maven projects. Setting `java.version` in
the POM selects the compiler target; it does not select the JDK running Maven.

## JWT signing key

Running the application requires `JWT_SECRET`, a Base64-encoded random
key of at least 32 bytes. For a local PowerShell session, run this from `backend`:

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
`backend/src/main/resources/application.properties`. The default database is
`ecommerce_db` on localhost port 3308. Full backend verification includes a
context test that requires access to this database. The commands above disable
automatic schema updates.

For legacy user tables matching the inspected schema, apply
`backend/sql/001_registration_user_columns.sql` explicitly before registration.
It enables generated user IDs and allows NULL in the unused legacy password
column while retaining `password_hash`. The current local database is already
migrated. This script is not an initial schema or an automatic migration runner.

With the backend running on port 8080, open a second PowerShell terminal:

```powershell
cd frontend
npm ci
npm start
```

Open `http://127.0.0.1:4200/auth/login` or `/auth/register`. The development proxy
forwards `/api/**` to Spring Boot. Registration creates a CUSTOMER account and
offers sign-in. Successful login opens `/account`; ADMIN roles are confirmed by
the server, and `/api/admin/**` requires ADMIN.

## Frontend checks

```powershell
cd frontend
npm run build
npm test -- --watch=false
```

The production frontend is written to `frontend/dist/frontend/browser`; Maven
verification packages the backend at `backend/target/e-commerce-0.0.1-SNAPSHOT.jar`.
The existing Nginx configuration expects a backend host named `ecommerce-backend`
on port 8080. No lint task or automated browser-test runner is configured.

Sessions use an in-memory JWT and end on page refresh. Client logout clears that
state; issued tokens expire after 15 minutes and are not revoked on logout.
Password recovery, social login, persistent sign-in and refresh restoration are
not implemented. The storefront and admin product-management UI remain future work.

See `frontend/AUTH_ARCHITECTURE.md` and the batch reports under `frontend/review/`
for architecture, file inventories, screenshots and validation evidence.
