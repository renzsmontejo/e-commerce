# Authentication architecture

## Scope and decisions

Angular 21 standalone components and route-level lazy loading, using the project's
existing CSS setup. No additional UI framework or NgModules. The root renders only
the router outlet. `/` redirects to `/auth/login`; unknown URLs have the same
fallback. `/auth` owns the minimal black brand header and main content landmark.
Batch B supplies the login UI; Batch C connects it to Spring Boot; Batch D adds
registration. See the reports in `review/batch-b/`, `review/batch-c/`, and
`review/batch-d/` for file inventories and validation.

Batch E fixes route focus, landmarks, live-region placement, validation-focus
timing, semantic form order, input-border contrast and stale API error handling.
Its browser measurements and regression results are in `review/batch-e/REPORT.md`.

Batch F completes local validation and handover. `review/batch-f/REPORT.md` contains
startup instructions and limitations; `review/batch-f/validation.json` records the
final 25 frontend tests, 13 backend tests, build artifacts and checksums.

The reference brand is We Shop Smarter. There is no storefront implementation or
standalone logo asset yet, so the header uses text and has no dead return-to-store
link. Product navigation remains future work. The login visual reuses the sofa
region of the supplied screenshot. Shared values live in `src/styles/tokens.css`;
reusable input styles live in `src/styles/forms.css`. Component CSS owns the layout.

## API configuration

`API_BASE_URL` reads `environment.apiBaseUrl`. Both environments currently use
`/api`. The development build replaces `environment.ts` with
`environment.development.ts`. `npm start` proxies `/api/**` to local port 8080;
the existing Nginx configuration proxies production requests to the backend.
Deployment addresses stay outside feature components.

`core/auth/auth.models.ts` matches the existing login and registration DTOs and
defines an in-memory authentication state contract. Passwords are request data
only and are never part of authentication state. `AuthService` calls login and
then `/api/auth/me` to confirm identity and roles. The interceptor attaches tokens
only to the configured API boundary, excluding public authentication endpoints.
Auth and guest guards protect `/account` and redirect signed-in users from login.
Safe return URLs currently allow only `/account` with optional query and fragment.

## Backend limitations for integration

- Login returns a bearer access token with a 15-minute lifetime and no roles.
- The JWT filter now verifies signatures and expiration, loads active users and
  roles from the database, and protects `/api/admin/**` with the ADMIN role.
- There is no refresh or session endpoint. In-memory state cannot survive a full
  page refresh. Secure restoration and persistent sign-in need backend support.
- No recovery/reset or social provider endpoints are available.
- Registration uses `/api/auth/register` and always assigns CUSTOMER server-side.
  The local user-table mismatch is corrected by the explicit migration
  `backend/sql/001_registration_user_columns.sql`. Apply it to matching legacy
  databases before using registration; it is not an automatic startup migration.

No JPA entities were changed. Batch D updates the legacy user-table definitions
while preserving existing data and foreign keys. Existing login and registration
DTOs remain unchanged; `/api/auth/me` is additive. Logout clears the client token;
it does not revoke an already issued token on the server. Tokens expire after 15
minutes. Role guards for future admin pages should accompany those actual routes;
server role enforcement is already in place. Recovery remains deferred because
the backend has no recovery API. Registration confirms success and directs the
user to sign in; it does not establish an authenticated session automatically.

## Run and verify

From `frontend`:

```powershell
npm ci
npm start
npm run build
npm test -- --watch=false
```

Open `/auth/login`. Integration requires Spring Boot on port 8080 with `JWT_SECRET`
set to a persistent Base64-encoded key of at least 32 random bytes. The backend
reads database settings from `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` (local
defaults are in application.properties). Use `--spring.jpa.hibernate.ddl-auto=none`
to avoid schema updates while testing the existing database. There is no lint task.
The reports describe unit, security, responsive, and real browser checks performed.

## Original Batch A file inventory and validation

Validation completed: locked dependencies installed, production build passed
(190.06 kB initial bundle), and both route tests passed. The first test attempt
was prevented by a sandbox temporary-file rename permission error; the rerun
outside that sandbox passed. `git diff --check` passed. No lint task is configured.
No responsive visual or backend integration tests were performed in Batch A.

Created:

- `src/app/features/auth/auth.routes.ts`
- `src/app/features/auth/pages/login/login.ts`
- `src/app/layouts/auth-layout/auth-layout.ts`, `.html`, `.css`
- `src/app/core/auth/auth.models.ts`
- `src/app/core/config/api.config.ts`
- `src/environments/environment.ts`, `environment.development.ts`
- `src/styles/tokens.css`
- `proxy.conf.json`
- `AUTH_ARCHITECTURE.md`

Modified:

- `src/app/app.ts`, `app.html`, `app.routes.ts`, `app.spec.ts`
- `src/styles.css`, `src/index.html`
- `angular.json`

Batch B, Batch C, and registration in Batch D are now implemented. Recovery stays deferred.
