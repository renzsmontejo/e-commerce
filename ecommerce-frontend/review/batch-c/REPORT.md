# Batch C — authentication integration

## Result

The login page now sends credentials to the existing Spring Boot login endpoint,
then confirms the returned token through the new authenticated `/api/auth/me`
endpoint. Successful login opens a small protected `/account` page showing the
server-confirmed email and roles and offering sign-out. This provides a real
post-login destination while storefront and admin features remain unimplemented.

## Security and behavior

- Typed login form stays presentation-only; a lazy login-page container handles
  requests, loading, errors, safe navigation, and subscription cancellation.
- AuthService stores the token and user only in memory. Credentials are not logged
  or persisted. Reloading the page requires signing in again and preserves `/account`
  as the intended destination. Persistent sign-in/refresh tokens remain unsupported.
- Expiration clears idle sessions; a protected API 401 clears the matching session
  without retry loops. A 403 reports permission denial without logging the user out.
- Auth and guest guards protect account access and keep signed-in users out of login.
  Return URLs allow only the currently implemented internal account destination.
- The interceptor adds a Bearer header only within the configured API boundary,
  excludes login/registration, and preserves explicit authorization headers.
- Passwords are sent unchanged; email is trimmed and normalized. Network/server
  failures produce safe messages, invalid credentials remain generic, and repeat
  submissions are blocked while busy.
- The JWT filter verifies signatures and expiry, rejects inactive/deleted users,
  and reloads roles from the database. `/api/admin/**` requires ADMIN on the server.
  It runs once inside Spring Security rather than as a second servlet filter.
- Login now rejects inactive users and no longer prints email to the console.
- Existing login/registration DTO contracts and JPA entities are unchanged. The
  new current-user endpoint exposes only id, email, and roles.
- Logout clears client state and timers. Existing bearer tokens remain valid on
  the server until expiration; no revocation mechanism is claimed.

## Files created

Frontend:
- `src/app/core/auth/auth.service.ts`, `auth.service.spec.ts`
- `src/app/core/auth/auth.interceptor.ts`, `auth.guards.ts`, `return-url.ts`
- `src/app/features/auth/pages/login/login-page.ts`, `login-page.spec.ts`
- `src/app/features/account/account.ts`
- `review/batch-c/REPORT.md`, `account.png`

Backend:
- `auth/dto/CurrentUserResponse.java`
- `src/test/java/com/revs/ecommerce/security/AuthenticationSecurityTests.java`
- `src/test/java/com/revs/ecommerce/security/LoginServiceTests.java`

## Files modified

Frontend:
- `core/auth/auth.models.ts`
- `features/auth/auth.routes.ts`
- `features/auth/pages/login/login.ts`, `login.html`, `login.spec.ts`
- `app.config.ts`, `app.routes.ts`, `app.spec.ts`
- `AUTH_ARCHITECTURE.md`

Backend (under `src/main/java/com/revs/ecommerce`):
- `security/JwtService.java`, `security/JwtAuthenticationFilter.java`
- `config/SecurityConfig.java` (preserved the user's existing error-dispatch configuration)
- `repositories/UsersRepository.java`
- `auth/service/AuthService.java`, `auth/controller/AuthController.java`

## Validation

- Angular production build passed without budget warnings.
- All 16 frontend tests passed: form behavior, container loading/network failure,
  success redirect, auth/guest guards, session confirmation, token scoping, invalid
  credentials, rejected identity request, expiry, logout race, safe redirects, 401/403.
- Backend compiled on JDK 27; all five targeted security/service tests passed.
  They exercise valid, tampered, malformed, expired and missing JWTs, inactive and
  deleted users, authenticated identity response, server ADMIN enforcement, and
  rejection of disabled accounts despite a matching password.
- Real browser + Spring Boot + existing MySQL: invalid credentials displayed a
  generic error; the previously created ADMIN account signed in successfully;
  `/account` displayed `admin@example.com` and ADMIN; sign-out returned to login;
  refreshing a new authenticated session returned to login with `/account` preserved.
- The live backend was launched with schema updates disabled and security logging
  reduced to INFO. No migrations or database writes were needed for these checks.
- `git diff --check` passed. No lint script exists. Broader cross-browser testing
  and accessibility review belong to Batch E/F.

## Run locally

Set `JWT_SECRET` to a Base64-encoded random signing key with at least 32 bytes.
Keep it stable for a deployment; do not commit it. Select JDK 27, then from backend:

```powershell
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=none --logging.level.org.springframework.security=INFO'
```

From frontend, `npm start`, then open `http://127.0.0.1:4200/auth/login`.
The proxy forwards `/api/**` to Spring Boot on port 8080. The existing local database
defaults are configured in backend application.yml. Use the admin account
created earlier; no credentials are embedded in frontend source.

## Remaining work

Registration/recovery UI requires its separate Batch D approval. Registration's
legacy schema mismatch remains unresolved; no recovery API exists. Secure refresh
restoration, persistent sign-in, and server token revocation need a separately
designed backend session lifecycle. No fake recovery or social controls were added.
