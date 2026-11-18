\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;
SELECT pg_advisory_xact_lock(782341905);
\ir migrations/032_collection_challenges.sql
\ir verify.sql
COMMIT;
