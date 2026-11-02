# Batch E — security, accessibility and responsive verification

## Findings fixed

1. Old or explicitly authorized API requests could show a forbidden notice in a
   newer session. Both 401 and 403 handling now require that the failed request
   used the current session's token. Regression tests cover stale responses and
   preserved explicit Authorization headers.
2. Route changes left keyboard focus on the previous page or document body.
   The root now focuses the rendered route's primary heading after navigation,
   without adding it to the normal Tab sequence.
3. The account page lacked a main landmark. It now has one and a responsive width
   that leaves side spacing while wrapping long email addresses.
4. Loading live regions were inside aria-busy forms, which can defer announcements.
   The stable status regions now sit outside those busy containers.
5. Validation could focus inputs before their associated errors were rendered.
   Login and registration now render errors before moving focus.
6. Input borders had insufficient visual distinction from white. A dedicated
   control-border token now measures 3.22:1 against the input surface while card
   borders retain the existing subtle styling.
7. The login's document order placed marketing before the form despite the mobile
   visual order. The form now comes first semantically; explicit desktop grid
   placement preserves the reference's left marketing/right form design.
8. The registration sign-in link was approximately 43px wide. Padding now gives
   it an approximately 51px-wide, 44px-high target. Registration email also disables
   autocapitalization and spellchecking explicitly.

## Security checks

- Tokens remain in memory; no localStorage/sessionStorage persistence or credential
  console logging exists in the authentication feature.
- Auth/guest guards and safe internal return URLs are covered by route tests.
- Requests to other origins and API-lookalike paths receive no session token.
- Public login/registration requests exclude automatic bearer headers.
- Protected-request 401 clears only the matching session, without refresh loops;
  403 preserves that session and reports permission denial.
- Malformed, expired and mismatched login/current-user responses never establish
  a session. Late login responses cannot restore a logged-out session.
- Server tests verify signature, expiry, required expiration, valid subject,
  active/existing users, and current database roles. A signed token with an ADMIN
  claim cannot grant a CUSTOMER access to an admin endpoint.
- Existing server CUSTOMER-only registration and password-boundary tests still pass.
- No production backend code, JPA entity, schema, or user data changed in Batch E.

The existing limitations remain: refresh requires sign-in because sessions are
in memory, client logout does not revoke already-issued tokens, and there is no
password recovery backend. These behaviors are unchanged and documented in the
architecture report.

## Browser and accessibility checks

Used the in-app browser's actual DOM, computed styles, screenshots and keyboard
input. `dom-audit.json` records measurements and the login keyboard trace.

Login and registration were checked at 1440, 1024, 768, 375 and 320px widths:

- No horizontal page overflow.
- Exactly one main landmark and one primary heading on each page.
- Every input had an explicit associated label.
- No duplicate IDs or broken aria-describedby references, including error states.
- Live loading regions had no aria-busy ancestor.
- Label text contrast measured 17.74:1 and control borders 3.22:1 against white.
- Visible buttons and auth links measured at least 44px high. All buttons measured
  at least 44px wide; the registration link was increased from 43px to about 51px.
- Heading focus moved to login, registration and account after route changes.
- Shift+Tab reached the visible skip link; Enter focused main content.
- Tab order reached email, password, visibility toggle, sign-in, then sign-up.
- Space operated the visibility toggle without submitting the form.
- Enter on the submit button showed validation and focused the first invalid input
  with its error description already present.
- At a 375×360 short viewport, keyboard focus scrolled the password toggle into
  view. This is a short-viewport check, not a physical on-screen keyboard test.
- The account page wrapped the existing long test email without overflow at 320px;
  keyboard sign-out returned to the login heading.
- Reduced-motion handling was reviewed in the existing spinner CSS. No OS preference
  or browser security setting was changed.

These checks did not include NVDA/VoiceOver, other browser engines, physical mobile
devices, or a full WCAG conformance audit.

## Tests and build

- Angular production build passed without warnings: 255.80 kB initial JS/CSS.
- All 25 frontend tests passed, including new route-accessibility and security
  regression cases.
- All 12 targeted backend security/service tests passed (six authentication-security,
  one login-service, five registration-service tests).
- Existing real CUSTOMER account signed in and out successfully after the fixes;
  no additional smoke-test accounts were created.
- `git diff --check` passed. No lint task is configured.

## Files created

- `src/app/accessibility.spec.ts`
- `review/batch-e/REPORT.md`, `dom-audit.json`
- `review/batch-e/login-1440.png`, `login-320.png`, `login-keyboard-errors.png`
- `review/batch-e/register-1440.png`, `register-320.png`, `account-320.png`

## Files modified

- `src/app/app.ts` — route-heading focus
- `src/app/core/auth/auth.interceptor.ts`, `auth.service.spec.ts` — session-bound errors
- `src/app/features/account/account.ts` — landmark and narrow-screen spacing
- `src/app/features/auth/pages/login/login.ts`, `.html`, `.css` — error rendering,
  loading region and semantic form order
- `src/app/features/auth/pages/register/register.ts`, `.html`, `.css` — error rendering,
  loading region, email attributes and touch target
- `src/styles/tokens.css`, `src/styles/forms.css` — accessible input boundary
- `ecommerce-backend/src/test/java/com/revs/ecommerce/security/AuthenticationSecurityTests.java`
- `AUTH_ARCHITECTURE.md`

Batch F remains the next approval checkpoint for final validation and handover.
