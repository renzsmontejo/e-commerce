# Authentication API

All requests use JSON. Protected requests use `Authorization: Bearer <accessToken>`.

| Method | Path | Request | Success |
| --- | --- | --- | --- |
| POST | /api/auth/register | firstName, lastName, email, password, optional phone | 201; registered customer |
| POST | /api/auth/login | email, password | 200; accessToken, tokenType, userId, email |
| POST | /api/auth/forgot-password | email | 200; generic message |
| POST | /api/auth/reset-password | token, password | 200; message |
| POST | /api/auth/logout | No body; bearer token required | 204 |

Passwords must contain 8–72 characters and at most 72 UTF-8 bytes for registration and reset. Invalid input returns 400; invalid login or bearer credentials return 401; duplicate registration returns 409.

Configure SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_AUTH, SMTP_STARTTLS, MAIL_FROM, and PASSWORD_RESET_URL for delivery. Defaults use localhost:1025 and http://localhost:4200/auth/reset-password. The Angular reset page must read the token query parameter and submit it with the new password. Raw reset tokens are sent only by email, expire after 30 minutes, and are stored as SHA-256 hashes. A successful reset invalidates all existing reset and access tokens. Logout invalidates only the supplied access token. Access tokens expire after 15 minutes.

Existing JWTs issued before access-token tracking was added require a fresh login. Refresh tokens are not implemented in this change.
