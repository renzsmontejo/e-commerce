# Final handover — authentication

## Delivered

The Angular frontend now has responsive login and registration pages matching the
provided storefront references, a shared auth layout, typed reactive forms,
accessible validation, visibility toggles, loading/error states and safe navigation.
The architecture uses standalone components, lazy routes, shared CSS tokens,
environment-driven API configuration, an in-memory AuthService, an HTTP interceptor,
and auth/guest guards.

Spring Boot authenticates BCrypt passwords, rejects inactive users, verifies JWT
signatures/expiry, checks current database roles and exposes `/api/auth/me` without
password data. Registration always creates CUSTOMER accounts. `/api/admin/**`
requires ADMIN. The protected `/account` page confirms identity/roles and offers
sign-out. Existing login/registration payload shapes remain compatible; registration
validation also enforces email length and BCrypt's UTF-8 password limit.

The local database migration enables generated user IDs and permits NULL in the
unused legacy password column. Existing data and JPA entities were preserved.
The ADMIN account created earlier remains available. One CUSTOMER smoke-test
account remains; its identity is documented in the Batch D report.

## Final verification

| Check | Result |
| --- | --- |
| Angular production build | Passed; 255.80 kB initial JS/CSS |
| Frontend tests | 25 passed across six test files |
| Full Maven verify | Passed; runnable Spring Boot JAR produced |
| Backend tests | 13 passed: context 1, security 6, login 1, registration 5 |
| Backend context test | Connected to local MySQL; schema updates disabled |
| Prettier check | Passed for app source, shared styles, proxy and Angular config |
| Whitespace check | `git diff --check` passed |
| Lint | No configured task |
| Browser evidence | Batch E: keyboard, labels, landmarks, contrast, reflow at 320–1440px |
| Container base images | Java 27 JDK and JRE registry manifests verified |
| Full container builds | Not run |

`validation.json` records the test-suite summaries, installed frontend versions,
verified commands and SHA-256 hashes for the backend JAR and frontend index.
No dependency versions or lockfiles were changed by this implementation.

Build outputs:

- `ecommerce-frontend/dist/ecommerce-frontend/browser/`
- `ecommerce-backend/target/e-commerce-0.0.1-SNAPSHOT.jar`

## Start locally

1. Start the existing MySQL instance. Supply `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
   as needed; existing local defaults point to `ecommerce_db` on port 3308.
2. Select an installed JDK 27 through `JAVA_HOME`. The machine used for verification
   has `C:\Program Files\Java\jdk-27.0.1.0.2`.
3. Set `JWT_SECRET` to a Base64-encoded random key of at least 32 bytes. The root
   README includes a local key-generation example. Deployments must keep this key
   stable and supply it outside source control.
4. From backend, run:

   ```powershell
   .\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none --logging.level.org.springframework.security=INFO'
   ```

5. From frontend in another terminal, run `npm ci`, then `npm start`.
6. Open `http://127.0.0.1:4200/auth/login`. Sign up at `/auth/register`, then sign in.
   Successful login opens `/account`.

The existing local database is already migrated. Before using another matching
legacy database, review and explicitly apply
`ecommerce-backend/src/main/resources/db/migration/001_registration_user_columns.sql`. It is not an initial database
schema or an automatic migration runner. Backend verification also requires an
accessible database for the context test; the test generates its own JWT key.

## Session and feature boundaries

- Tokens live only in memory; refreshing requires sign-in and preserves a safe
  account destination. No localStorage/sessionStorage credentials or tokens exist.
- Tokens expire after 15 minutes. Client logout clears state/timers; it does not
  revoke an already-issued token on the server.
- Password recovery/reset, email delivery, social providers, persistent sign-in
  and secure refresh restoration are not implemented. No placeholder controls
  imply that these features work.
- The reference storefront and admin product-management pages were design context;
  implementing those full features was outside these authentication batches.
- The screenshot-derived sofa image is adequate for the current layout; the original
  high-resolution product photo would improve sharpness on larger displays.
- Browser checks used one in-app browser engine and DOM/keyboard inspection. Actual
  screen-reader testing, other engines and physical mobile keyboards remain unverified.

## Batch F changes

Created:
- `ecommerce-frontend/review/batch-f/REPORT.md`
- `ecommerce-frontend/review/batch-f/validation.json`

Modified:
- `ecommerce-frontend/.dockerignore` — preserved existing exclusions and added review artifacts
- Root `README.md` — complete run/test instructions, actual local JDK path, session
  boundaries and migration guidance
- `ecommerce-frontend/README.md` — replaced starter instructions with the real route/API setup;
  removed the unsupported `ng e2e` instruction
- `ecommerce-frontend/AUTH_ARCHITECTURE.md` — final verification reference
- `ecommerce-frontend/angular.json` — formatting only
- `ecommerce-backend/Dockerfile` — Java 25 changed to Java 27 in build and runtime stages

The Dockerfile mismatch would prevent compiling/running the Java 27 target with
Java 25. Both replacement image tags were verified against registry manifests;
container execution is not claimed. The frontend Docker ignore file already excluded
host node_modules and build output; it now excludes review files from the image build
context. Nginx still expects `ecommerce-backend:8080` on its configured network.

Pre-existing user changes, including application configuration, configuration notes
and unrelated untracked files, were preserved. No commit, push, deployment or new
scheduled work was performed. All approved authentication batches are complete;
unsupported recovery functionality remains explicitly deferred.
