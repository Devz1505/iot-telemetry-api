-- Creates the app's own login and database. Safe to run more than once.
-- Use the same password as in db.properties (replace change-me below).
-- Run as the postgres superuser (psql asks for that password):
--   psql -U postgres -h localhost -f local/setup-db.sql
SELECT 'CREATE ROLE telemetry_app LOGIN PASSWORD ''change-me'''
WHERE NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'telemetry_app')\gexec
SELECT 'CREATE DATABASE telemetry OWNER telemetry_app'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'telemetry')\gexec
\echo 'Done: role telemetry_app and database telemetry are ready.'
