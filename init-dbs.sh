#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE guida_auth;
    CREATE DATABASE guida_clients;
    CREATE DATABASE guida_invoices;
    CREATE DATABASE guida_afip;
EOSQL
