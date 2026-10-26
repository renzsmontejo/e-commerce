# We Shop Smarter frontend

Angular 21 standalone authentication UI with lazy-loaded routes and the existing
CSS styling system. No additional UI framework is used.

## Run locally

Start the Spring Boot backend on port 8080 first (see the root README for JDK,
MySQL, signing-key and migration instructions). From this folder:

```powershell
npm ci
npm start
```

Open `http://127.0.0.1:4200/auth/login`. The development server proxies `/api/**`
to `http://localhost:8080`, so no cross-origin configuration is needed locally.

| Route | Behavior |
| --- | --- |
| `/auth/login` | Email/password login with validation and safe return destination |
| `/auth/register` | CUSTOMER registration, confirmation and sign-in link |
| `/account` | Guarded current user/roles and sign-out |
| `/` or unknown path | Redirects to login; an active session continues to account |

A persistent backend signing key is required outside temporary local testing.
No credentials are hardcoded in this frontend. Use an existing account or register
a CUSTOMER. ADMIN creation remains an administrative database operation.

## Verify

```powershell
npm run build
npm test -- --watch=false
npx --no-install prettier --check src/app src/styles src/styles.css proxy.conf.json angular.json
```

The production output is `dist/frontend/browser`. There is no lint script and no
configured `ng e2e` target. Browser checks were performed through the in-app browser;
the reports describe exactly what was checked and what remains unverified.

## API and session behavior

- `POST /api/auth/login` returns a bearer token; `GET /api/auth/me` confirms it
  before the frontend establishes a session.
- `POST /api/auth/register` creates a CUSTOMER and returns registration details.
  It does not log the user in automatically.
- Tokens and user state live only in memory. Refresh requires signing in again.
- Logout clears client state. Server tokens remain valid until their 15-minute expiry.
- The interceptor scopes bearer headers to the configured API and handles matching
  session 401/403 responses without refresh loops.
- Environment files supply `/api`; the deployment proxy selects the actual backend.
- Recovery/reset, social providers and persistent sign-in have no backend support
  and are omitted from the UI.

## Build container

The frontend Dockerfile builds using Node 22 and serves through Nginx. The
`.dockerignore` keeps host dependencies and review screenshots out of the build
context. Nginx expects `ecommerce-backend:8080` on its container network and falls
back to index.html for Angular deep links. Container builds are separate from the
verified local Angular build; consult the final report for container limitations.

## Review materials

`AUTH_ARCHITECTURE.md` documents the structure and configuration. `review/batch-b/`
through `review/batch-e/` contain implementation reports and screenshots. The final
handover and verification results are in `review/batch-f/REPORT.md`.
