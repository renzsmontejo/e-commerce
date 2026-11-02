# Batch B — responsive login UI

## Result and decisions

Implemented a desktop two-column login page with the reference's black header,
white form, purple accent, warm neutral visual panel, and navy sofa. The image is
the supplied storefront screenshot, displayed through a CSS crop of its furniture
region. The original reference is preserved. The current image is screenshot
resolution; an original product photo would improve sharpness on large screens.

Below 768px the form comes first and decorative photography is hidden. The page
uses the existing CSS tokens, Grid and Flexbox, with reusable auth input styles in
`src/styles/forms.css`. No UI library was added.

## Authentication boundary

Typed reactive form validates required email, email format, and required password.
Passwords keep their original value and have no new-password length rules. Fields
support autofill; the password toggle is keyboard operable. Validation connects
errors through `aria-describedby` and focuses the first invalid input.

The component exposes `submitting`, `authenticationError`, and `loginSubmitted`
bindings for Batch C. Loading disables submission and makes inputs read-only;
spinner motion respects reduced-motion preferences. Authentication errors display
a generic message without exposing backend details.

No authentication request is made yet. A valid submission explicitly states that
credentials were not sent. No fake success, token storage, or timed login is used.
Registration, recovery, social providers, and persistent sign-in controls are
deferred until real routes and backend support exist. The skip link focuses main
content directly, avoiding a navigation caused by the application's base URL.

## File inventory

Created:
- `src/app/features/auth/pages/login/login.html`
- `src/app/features/auth/pages/login/login.css`
- `src/app/features/auth/pages/login/login.spec.ts`
- `src/styles/forms.css`
- `public/images/storefront-reference.png` (copy of supplied screenshot)
- `review/batch-b/REPORT.md`, `desktop.png`, `mobile-errors.png`

Modified:
- `src/app/features/auth/pages/login/login.ts`
- `src/app/layouts/auth-layout/auth-layout.html`
- `src/styles.css`
- `AUTH_ARCHITECTURE.md`

## Validation

- Production build passed, without budget warnings (214.27 kB initial JS/CSS).
- All six tests passed: two routing tests and four login tests covering validation,
  submission data, password visibility, busy state, and generic error rendering.
- Browser screenshots inspected at 1440, 1024, 768, 375, and 320px widths.
- DOM measurements confirmed no horizontal page overflow at every tested width.
- At narrow widths the form precedes the welcome panel and imagery is hidden.
- Validation errors inspected on mobile; browser check confirmed first-invalid-field
  focus and no overflow. Saved review images show desktop and mobile error states.
- `git diff --check` passed. There is no configured lint task.

Real mobile keyboard behavior, full screen-reader review, and live authentication
remain unverified. Batch C requires approval before connecting the backend; existing
JWT filter/session restoration limitations are documented in `AUTH_ARCHITECTURE.md`.

Preview: `npm start`, then open `http://127.0.0.1:4200/auth/login`.
