-- Additive migration: legacy users remain intact and cannot be claimed by registration.
BEGIN;
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash TEXT;
CREATE UNIQUE INDEX IF NOT EXISTS users_email_lower_key ON users (lower(email));
COMMIT;
