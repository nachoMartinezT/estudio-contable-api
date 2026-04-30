#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE guida_auth'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'guida_auth')\gexec

    SELECT 'CREATE DATABASE guida_clients'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'guida_clients')\gexec

    SELECT 'CREATE DATABASE guida_invoices'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'guida_invoices')\gexec

    SELECT 'CREATE DATABASE guida_afip'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'guida_afip')\gexec
EOSQL
