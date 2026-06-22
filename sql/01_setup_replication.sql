-- Create replication user
CREATE USER replicator WITH REPLICATION ENCRYPTED PASSWORD 'replica_password';
SELECT * FROM pg_create_physical_replication_slot('replica_slot');
