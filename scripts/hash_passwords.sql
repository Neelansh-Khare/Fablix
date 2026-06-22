-- Migrate plaintext passwords to BCrypt hashes
-- This script identifies passwords that are NOT BCrypt hashed and marks them for update

-- BCrypt hashes always start with $2a$, $2b$, or $2y$ and are 60 characters long
-- Plaintext passwords don't match this pattern

-- First, let's see which customers have plaintext passwords
SELECT id, email,
       CASE
           WHEN password ~ '^\$2[aby]\$' AND LENGTH(password) = 60 THEN 'BCrypt'
           ELSE 'Plaintext'
       END as password_type,
       password
FROM customers
WHERE NOT (password ~ '^\$2[aby]\$' AND LENGTH(password) = 60)
LIMIT 20;

-- NOTE: To actually hash passwords, you need to run the Java migration utility
-- because BCrypt hashing requires the jBCrypt library.
--
-- Run: ./hash_passwords.sh
-- Or manually update passwords via the application's registration endpoint
