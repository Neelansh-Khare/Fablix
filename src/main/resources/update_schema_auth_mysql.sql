-- MySQL-compatible version: Add role column to customers table
-- For MySQL, we can't use IF NOT EXISTS with ADD COLUMN
-- So we need to check manually or just run and ignore error if column exists

-- Add role column (will error if already exists, which is OK to ignore)
ALTER TABLE customers ADD COLUMN role VARCHAR(20) DEFAULT 'customer';

-- If you get "Duplicate column name" error, the column already exists - ignore it