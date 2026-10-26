-- MySQL 8: align the existing users table with the current Users entity.
-- Existing IDs, password hashes, legacy password values, and foreign keys remain intact.
-- Apply explicitly to the intended database; Hibernate schema updates are unnecessary.
-- MySQL blocks changes to referenced columns while FK checking is enabled, even
-- when BIGINT's type stays identical. Suspend checks only on this DDL connection;
-- no data is inserted, updated, or deleted while they are suspended.
SET @registration_previous_fk_checks = @@SESSION.FOREIGN_KEY_CHECKS;
SET SESSION FOREIGN_KEY_CHECKS = 0;
ALTER TABLE users
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT,
    MODIFY COLUMN password VARCHAR(200) NULL;
SET SESSION FOREIGN_KEY_CHECKS = @registration_previous_fk_checks;

-- Check the result after applying:
-- SHOW CREATE TABLE users;
