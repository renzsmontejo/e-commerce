# Cart and checkout API

All six endpoints require `Authorization: Bearer <accessToken>`. The authenticated user owns the cart; user IDs from request bodies are never used.

| Method | Endpoint | Request | Success |
| --- | --- | --- | --- |
| GET | /api/cart | No body | 200 cart |
| POST | /api/cart/items | variantId, quantity | 201 updated cart |
| PATCH | /api/cart/items/{id} | quantity | 200 updated cart |
| DELETE | /api/cart/items/{id} | No body | 204 |
| POST | /api/checkout/preview | No body | 200 validated items and totals |
| POST | /api/orders | Shipping details and expectedTotal | 201 order |

## Cart

```json
{ "variantId": 1, "quantity": 2 }
```

Adding the same variant increases its existing quantity. PATCH sets its absolute quantity, for example `{ "quantity": 3 }`. Quantities must be integers from 1 to 999; use DELETE to remove an item. A cart supports up to 100 distinct variants. Item IDs in PATCH/DELETE refer to cart-item IDs returned by the cart, not variant IDs. Missing items and other users' items both return 404.

GET returns `id`, `items`, `totalQuantity`, `subtotal`, and `currency`. Each item includes product/variant identity, name, SKU, current unit price, line total, available quantity, and `purchasable`. An empty first-time GET returns an empty cart without creating a database row. An existing unavailable item remains visible so it can be removed. Add/change validates product, category, variant status and available stock; cart operations do not reserve stock.

## Preview and order

POST `/api/checkout/preview` calculates current totals and rejects an empty cart, unavailable items, or insufficient inventory. It does not create an order or reserve stock. Response includes `items` and `totals`: subtotal, discountAmount, shippingFee, taxAmount, totalAmount, currency.

POST `/api/orders` uses the current cart and accepts:

```json
{
  "recipientName": "Test Customer",
  "shippingPhone": "09123456789",
  "shippingAddress": "123 Example Street, Manila, Philippines",
  "expectedTotal": 1200.00
}
```

Use `totals.totalAmount` from the latest preview for `expectedTotal`. The server recalculates every amount from current database prices; the submitted total is only a comparison, never a pricing input. A changed total returns 409 so the client can show an updated preview. Recipient name, phone, and address are required and stored as order snapshots. Name is limited to 150 characters, phone to 50, address to 2000. The response contains order ID/number, PENDING status, shipping details, placedAt, calculated totals, and item snapshots.

Order placement runs in one database transaction: validate the cart; lock current product/variant/category state and inventory; save order and item price snapshots; reserve stock; save the initial status and inventory movements; clear the cart. Concurrent checkout cannot reserve more than available stock. Failed transactions restore both inventory and cart. Repeating an order immediately after successful placement returns 409 because the cart is empty.

## Pricing and inventory policy

- Currency is PHP, matching the existing order model; money uses two decimal places.
- Set CHECKOUT_SHIPPING_FEE for a flat shipping fee and CHECKOUT_TAX_RATE for a fractional tax rate (for example `0.12`). Both default to zero until store-specific rules are configured. Tax is calculated on subtotal and rounded to two decimal places; shipping is then added.
- Discount amount is zero; coupon processing is outside these endpoints.
- `inventories.quantity_on_hand - quantity_reserved` is the stock source. A missing inventory row means out of stock. The legacy `product_variants.stock_quantity` column is not used.
- PENDING orders increase quantity_reserved and keep quantity_on_hand unchanged. RESERVATION movements record negative changes to available stock. Reservations remain until future fulfillment/cancellation processing releases or consumes them. This endpoint does not capture payment or mark an order paid.

Errors: 400 malformed/invalid request; 401 unauthenticated; 404 missing variant/cart item; 409 empty cart, unavailable product, insufficient stock, stale total, or monetary overflow. All order data, totals, and inventory updates are server-owned.

## Local deployment

The verified local backend uses `ecommerce-backend:checkout`. Rebuild with JDK 27 using `mvn package` from `ecommerce-backend`, then run `docker build -f ecommerce-backend/Dockerfile.runtime -t ecommerce-backend:checkout ecommerce-backend` from the repository root. Preserve the existing database/JWT environment and product-image volume when replacing the container, then reload the frontend Nginx proxy. No additional schema migration is required for these six endpoints.
