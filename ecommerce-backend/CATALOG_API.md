# Products and categories API

Public endpoints require no token. Admin endpoints require `Authorization: Bearer <accessToken>` with the ADMIN role from the database.

| Method | Endpoint | Success |
| --- | --- | --- |
| GET | /api/products | 200 paginated products |
| GET | /api/products/{id} | 200 product details; 404 if missing, archived, or category inactive |
| GET | /api/categories | 200 active categories, sorted by name then ID |
| POST | /api/admin/products | 201 product and Location header |
| PUT | /api/admin/products/{id} | 200 updated product |
| PATCH | /api/admin/products/{id}/status | 200 updated product |
| POST | /api/admin/products/{id}/images | 201 array of image metadata |

## Search and pagination

Example: `GET /api/products?q=shirt&categoryId=1&minPrice=10&maxPrice=50&page=0&size=20&sort=name,asc`

- `q`: case-insensitive literal substring in product name or description; up to 200 characters.
- `categoryId`, `brandId`: optional positive IDs; category filter matches the exact category.
- `minPrice`, `maxPrice`: optional inclusive, nonnegative prices. At least one active variant must satisfy both bounds.
- `page`: zero-based, default 0; `size`: 1–100, default 20.
- `sort`: `name`, `createdAt`, or `id`, followed by `,asc` or `,desc`. Default `createdAt,desc`; ID provides a stable tiebreaker.

Response: `{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`.
Products include category/brand identity, ordered image metadata, and active variants (SKU, price, compare-at price, weight).
Only ACTIVE products in active categories are visible publicly. Products without variants are listed unless a price filter is supplied.

## Create and update

POST and PUT accept the same JSON; PUT fully replaces the editable metadata. Omit/null brandId to remove the brand.

```json
{
  "name": "Everyday Shirt",
  "slug": "everyday-shirt",
  "description": "Cotton shirt",
  "categoryId": 1,
  "brandId": null,
  "status": "ACTIVE"
}
```

Name, slug, categoryId, and status are required. Slugs must contain lowercase letters/numbers separated by single hyphens and be unique. Category and optional brand must exist; ACTIVE products require an active category. Status accepts ACTIVE or ARCHIVED. Updates preserve variants and images. Prices belong to the existing product_variants table; variant creation is outside these endpoints.

PATCH status accepts `{ "status": "ARCHIVED" }` or `{ "status": "ACTIVE" }`.

## Image uploads

Send multipart form data with 1–10 files under repeated `files` fields:

```shell
curl -X POST http://localhost:4200/api/admin/products/1/images -H "Authorization: Bearer TOKEN" -F "files=@front.png" -F "files=@back.jpg"
```

Each file must be a decodable PNG/JPEG, at most 5 MiB and 20 megapixels. The server verifies content and re-encodes the image, ignoring client filenames and metadata. Uploaded images receive random filenames and public URLs under `/api/product-images/`. The first uploaded image becomes primary when no primary exists; subsequent uploads append to the display order. Failed transactions clean up newly written files.

Set PRODUCT_IMAGE_DIRECTORY to configure storage; default `./uploads/product-images`. In Docker mount a persistent volume at `/app/uploads/product-images` (for example `-v ecommerce-product-images:/app/uploads/product-images`). Preserve this volume when replacing containers. Nginx and Spring both allow a total multipart request of 51 MiB. Local disk storage is intended for a single backend instance; shared storage is required for multiple instances.

Errors: 400 invalid body/query/image or missing category/brand; 401 unauthenticated admin access; 403 non-admin access; 404 missing product; 409 conflicting slug/related data; 413 upload too large.

## Existing legacy database

The inspected local database has extra legacy product columns. Flyway's conditional V2 compatibility migration adds defaults for unused required columns and permits an omitted description without changing existing values. Fresh schemas are created by V1. The former manual script is archived under `src/main/resources/db/legacy/002_legacy_product_defaults.sql` for historical reference.


Flyway now applies the conditional V2 Java migration automatically, including the legacy defaults above. The archived manual script is retained under `db/legacy` and should not be applied separately on a Flyway-managed database. Hibernate validates instead of updating the schema.
