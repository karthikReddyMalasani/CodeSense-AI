#!/bin/bash
# Initialize PostgreSQL extensions required by CodeSense AI
# This script runs automatically on first postgres container startup.
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE EXTENSION IF NOT EXISTS vector;
    CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
    SELECT 'PGVector extension installed: ' || extversion FROM pg_extension WHERE extname = 'vector';
EOSQL

echo "CodeSense AI: PostgreSQL extensions initialized."
