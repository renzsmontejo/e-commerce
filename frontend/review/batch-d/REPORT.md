# Batch D — registration

## Result and scope

Implemented the approved registration flow at `/auth/register`, linked from the
login form. It shares the black brand header, purple actions, CSS tokens and input
styles. Desktop uses a compact centered form with paired profile fields; narrow
screens use one column. No additional framework or JPA entity changes were needed.

The typed reactive form includes first/last name, email, optional phone, password
and confirmation. It validates blank names, field lengths, email format, minimum
password length, BCrypt's 72-byte UTF-8 limit, and matching passwords. Passwords
are never trimmed or persisted. Both password fields have keyboard-operable
visibility controls and password-manager autocomplete. Errors are associated with
their inputs, and invalid submission focuses the first invalid field.

Submission uses the existing `/api/auth/register` contract through AuthService,
normalizes profile fields, omits confirmation, and never accepts a requested role.
The server assigns CUSTOMER. Loading prevents repeat submissions. Conflict,
network, validation and server failures receive safe feedback. On success the
form clears its password values and offers sign-in, preserving the safe intended
destination. It does not log the user in automatically.

Recovery/reset pages remain deferred: there is no backend recovery API or email
delivery service. No nonfunctional links or fake recovery requests were introduced.

## Database and backend changes

Applied `backend/sql/001_registration_user_columns.sql` to the existing local
database. It adds AUTO_INCREMENT to `users.id` and makes the unused legacy
`users.password` nullable; the application continues storing BCrypt in
`password_hash`. Existing IDs, password values, hashes, data and FK definitions
were preserved. MySQL required suspending FK checking on the migration connection
while changing the referenced ID definition; checks were restored immediately.
No data was inserted or altered while checks were suspended.

Verification after migration: the existing user count remained one, foreign-key
checking was enabled, the ID column reported auto_increment, and no user-role
orphans existed. A subsequent real registration successfully used generated ID 2.

The migration is explicit, not automatically executed at application startup.
It targets the inspected MySQL legacy schema; review table definitions before
applying it to another database. The unchanged Users entity already uses IDENTITY.

RegisterRequest now validates email's database length and password UTF-8 byte
length before BCrypt encoding. AuthService translates a concurrent MySQL duplicate
key registration into HTTP 409 and flushes within that exception boundary.

## Files created

- `src/app/features/auth/pages/register/register.ts`, `.html`, `.css`, `.spec.ts`
- `backend/sql/001_registration_user_columns.sql`
- `backend/src/test/java/com/revs/ecommerce/security/RegistrationServiceTests.java`
- `review/batch-d/REPORT.md`, `register-1440.png`, `register-375.png`, `success.png`,
  `customer-account.png`

## Files modified

- `src/app/core/auth/auth.service.ts`
- `src/app/features/auth/auth.routes.ts`
- `src/app/features/auth/pages/login/login.ts`, `.html`, `.css`, `.spec.ts`
- `src/app/features/auth/pages/login/login-page.ts`
- `backend/src/main/java/com/revs/ecommerce/auth/dto/RegisterRequest.java`
- `backend/src/main/java/com/revs/ecommerce/auth/service/AuthService.java`
- `AUTH_ARCHITECTURE.md`
- Root `README.md` (correct signing-key environment variable)

## Validation

- Angular production build passed without budget warnings.
- All 21 frontend tests passed, including new registration coverage for required
  fields, focus, mismatched and multibyte passwords, payload normalization,
  duplicate-submit protection, success, conflicts, network errors and optional phone.
- All ten targeted backend security/service tests passed, including new tests
  for CUSTOMER-only assignment, encoded passwords, duplicate-email paths,
  multibyte password boundaries and missing role configuration.
- Browser inspected at 1440, 1024, 768, 375 and 320px. DOM measurements found no
  horizontal overflow. Narrow mobile validation remained readable and focused
  the first invalid input.
- Real browser + Spring Boot + MySQL: account creation succeeded, the success
  heading received focus, the new account signed in and showed CUSTOMER, and
  sign-out worked. Re-registering the same email displayed the conflict message
  and restored the submit button. Database metadata confirmed a 60-character BCrypt hash and
  NULL in the legacy password column.
- One local smoke-test account remains for inspection: user ID 2,
  `registration-check-1793029172148@example.com` (Registration Check, CUSTOMER).
  Its generated password is not stored in source or this report.
- `git diff --check` passed. There is no configured lint task.

## Run and remaining work

Run the existing Spring Boot backend and `npm start` from frontend, then open
`http://127.0.0.1:4200/auth/register`. The current local database has already been
migrated. Other matching legacy databases need the explicit SQL migration first.

Password recovery requires a separate backend design and implementation. Batch E
is the next approval checkpoint for broader security, accessibility and responsive
verification; Batch F is final validation and documentation.
