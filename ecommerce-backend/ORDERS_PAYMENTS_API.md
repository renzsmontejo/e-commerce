# Orders and payments API

## Order endpoints

Customer endpoints require a bearer JWT and use the authenticated owner. Other customers' order IDs return 404. Admin endpoints require the ADMIN role.

| Method | Endpoint | Success |
| --- | --- | --- |
| GET | /api/orders?page=0&size=20 | 200 customer order history |
| GET | /api/orders/{id} | 200 owned order details |
| POST | /api/orders/{id}/cancel | 200 cancellation request recorded |
| GET | /api/admin/orders?status=PENDING&page=0&size=20 | 200 all matching orders |
| PATCH | /api/admin/orders/{id}/status | 200 updated details |
| POST | /api/payments/checkout-session | 200 hosted session, when PayMongo credentials are configured |
| POST | /api/webhooks/payments | 200 acknowledged verified provider event, when PayMongo credentials are configured |

Pagination is zero-based; size is 1–100. History sorts newest first by placedAt then ID. Pages return content, page, size, totalElements, totalPages. Summaries include order ID/number, status, total amount, currency, and placement date. Details also include shipping snapshots, line items, totals, payment summaries, and status history. No provider secrets or raw card/payment data are returned.

Cancellation request:

```json
{ "reason": "I no longer need this item" }
```

The reason is required and limited to 1000 characters. PENDING, PAID, and PROCESSING orders can enter CANCELLATION_REQUESTED. Repeating the request is harmless. Stock stays reserved until an administrator approves cancellation. SHIPPED and DELIVERED orders cannot request cancellation here.

Admin status request:

```json
{ "status": "CANCELLED", "notes": "Cancellation approved" }
```

Notes are optional, up to 1000 characters. Valid transitions:

- PENDING -> CANCELLED (only without an active/successful payment).
- PAID -> PROCESSING -> SHIPPED -> DELIVERED.
- CANCELLATION_REQUESTED -> CANCELLED, or back to its preceding state when declined. If payment arrived while cancellation was pending, a prior PENDING state resumes as PAID.
- Repeating the current status is harmless; terminal CANCELLED/DELIVERED orders cannot be reopened.

Only a verified payment can move PENDING to PAID; administrators cannot manually mark an unpaid order paid. Active PayMongo sessions are explicitly expired through the provider API before cancellation approval; paid/processing sessions block approval. Paid orders require refund handling before cancellation; a refund endpoint is outside this change and approval remains blocked while a successful payment exists.

Cancellation releases reserved quantity without changing on-hand stock. Shipment consumes both reserved and on-hand quantity. Each change locks the order and affected inventory, updates stock once, and writes audit/status records in the same transaction.

## Payment integration status

PayMongo Checkout v2 is implemented in `payment/gateway/PayMongoGateway.java`. Configure these server-side environment variables:

| Variable | Purpose |
| --- | --- |
| PAYMONGO_SECRET_KEY | PayMongo secret API key (start with a test key) |
| PAYMONGO_WEBHOOK_SECRET | Signing secret for this webhook endpoint |
| PAYMONGO_LIVE_MODE | false for test mode; true only with a matching live secret key |
| PAYMONGO_PAYMENT_METHODS | Comma-separated enabled methods; default gcash,card |
| PAYMONGO_SUCCESS_URL | Browser return URL; default http://localhost:4200/account?payment=success |
| PAYMONGO_CANCEL_URL | Browser return URL; default http://localhost:4200/account?payment=cancelled |
| PAYMONGO_SIGNATURE_TOLERANCE_SECONDS | Signature timestamp tolerance; default 300 |

Register a publicly reachable HTTPS endpoint ending in `/api/webhooks/payments` in the PayMongo dashboard and subscribe to `checkout_session.payment.paid`. Use separate test/live endpoint signing secrets. Credentials stay on the backend. No keys are committed, and no real PayMongo account was charged during verification. With credentials absent, the payment routes return 503.

The adapter creates `/v2/checkout_sessions`, retrieves `/v1/checkout_sessions/{id}`, and expires sessions with `/v1/checkout_sessions/{id}/expire`. It converts saved PHP totals to integer centavos and submits a stable idempotency header. Provider fees are not passed on to customers, so the captured amount matches the saved order total. It reuses an active session, and recognizes a paid payment even if the session still reports active.

Webhook verification uses HMAC-SHA256 of `timestamp.rawBody`, checks the configured `te` (test) or `li` (live) signature with a constant-time comparison, enforces timestamp tolerance, then validates event mode. It supports the standard event envelope and the current hosted checkout envelope. The single paid payment in the Checkout Session determines captured amount/currency. Failed individual attempts are ignored because they do not close a Checkout Session. When an envelope has no event ID, the verified body hash is used for duplicate detection.

The application never trusts a browser return URL or client-reported payment status. Browser return URLs navigate back to the current account page; the frontend has no dedicated payment result UI yet.

Checkout session request:

```json
{ "orderId": 123 }
```

Only the owner of a PENDING order can start checkout. Amount/currency come from the saved order, not the request. An existing open session is reused. Provider creation must use the supplied stable idempotency key. Response contains paymentId, orderId, provider, checkoutUrl. A success callback arriving before the session database transaction commits receives a retryable non-2xx response.

The webhook route is public to providers, passes exact raw bytes and headers to the selected adapter, and accepts events only after verification. The service checks session reference, provider, amount, and currency against both payment and order. Duplicate event IDs are acknowledged without duplicate mutations. A successful event marks payment/order paid once. Later failure/expiration events never downgrade a paid payment. A late payment for a cancelled order, or a second paid attempt, is marked PAID_REVIEW_REQUIRED for refund/manual review without reopening the order or reserving stock again.

Errors: 400 invalid request/event mismatch/signature (adapter-defined), 401 missing customer JWT, 403 non-admin, 404 unowned/missing order, 409 invalid lifecycle transition/payment conflict, 503 missing PayMongo configuration; 502 provider/network failure.

## Provider references

- [Hosted Checkout v2](https://docs.paymongo.com/docs/payment-channels-hosted-checkout)
- [Checkout Session resource and centavos](https://docs.paymongo.com/reference/checkout-session-resource)
- [Get a session](https://docs.paymongo.com/reference/get_checkout_sessions)
- [Expire a session](https://docs.paymongo.com/reference/create_checkout_sessions_id_expire)
- [Structured webhook signature verification](https://docs.paymongo.com/docs/developer-tools-webhook-setup-management)
- [Idempotency headers](https://docs.paymongo.com/docs/developer-tools-best-practices-1)

## Local build and setup

The local backend image is `ecommerce-backend:orders-paymongo`. Existing database/JWT settings and the product-image volume are preserved. Payment settings are listed in `.env.example`; copy the needed values into an ignored environment file and pass them into the backend container when recreating it (Docker does not load `.env.example` automatically). No additional database migration is needed.

All backend tests, including signature checks and mocked PayMongo HTTP responses, run without actual PayMongo credentials. The live local smoke check verifies order lifecycle and rejection of unconfigured payment requests; it does not initiate real/test-account PayMongo payments. To finish account-level verification, provide the test secret key and endpoint signing secret via the backend environment, register the public HTTPS webhook, then complete a test-mode hosted checkout.
