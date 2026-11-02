-- MySQL 8: compatibility for legacy products tables containing brand/is_featured.
-- Apply only to legacy schemas (SHOW CREATE TABLE products first).
-- Existing values and foreign keys are preserved; future inserts can omit unused columns.
ALTER TABLE products
    ALTER COLUMN brand SET DEFAULT '',
    ALTER COLUMN is_featured SET DEFAULT 0,
    MODIFY COLUMN description TEXT NULL;
