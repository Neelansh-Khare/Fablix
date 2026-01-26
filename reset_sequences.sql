-- Reset all sequences after data population
-- Run this script AFTER importing movie-data.sql or any data with explicit IDs

-- Reset customers sequence to max ID + 1
SELECT setval('customers_id_seq', COALESCE((SELECT MAX(id) FROM customers), 0) + 1, false);

-- Reset sales sequence to max ID + 1
SELECT setval('sales_id_seq', COALESCE((SELECT MAX(id) FROM sales), 0) + 1, false);

-- Reset genres sequence to max ID + 1
SELECT setval('genres_id_seq', COALESCE((SELECT MAX(id) FROM genres), 0) + 1, false);

-- Verify sequences are correct
DO $$
DECLARE
    cust_max INT;
    cust_seq INT;
    sales_max INT;
    sales_seq INT;
    genres_max INT;
    genres_seq INT;
BEGIN
    SELECT COALESCE(MAX(id), 0) INTO cust_max FROM customers;
    SELECT last_value INTO cust_seq FROM customers_id_seq;

    SELECT COALESCE(MAX(id), 0) INTO sales_max FROM sales;
    SELECT last_value INTO sales_seq FROM sales_id_seq;

    SELECT COALESCE(MAX(id), 0) INTO genres_max FROM genres;
    SELECT last_value INTO genres_seq FROM genres_id_seq;

    RAISE NOTICE 'Customers: max_id=%, sequence=% (OK: %)', cust_max, cust_seq, cust_seq > cust_max;
    RAISE NOTICE 'Sales: max_id=%, sequence=% (OK: %)', sales_max, sales_seq, sales_seq > sales_max;
    RAISE NOTICE 'Genres: max_id=%, sequence=% (OK: %)', genres_max, genres_seq, genres_seq > genres_max;
END $$;
