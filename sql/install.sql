\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;
SELECT pg_advisory_xact_lock(782341905);
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM information_schema.tables WHERE table_schema='public' AND table_name NOT IN ('spatial_ref_sys') AND table_type='BASE TABLE') THEN
  RAISE EXCEPTION 'install.sql requires an empty application database. Use upgrade.sql for an existing database';
 END IF;
END $$;
\ir schema/001_schema.sql
\ir seed/002_reference.sql
\ir seed/003_rewards_quests.sql
\ir migrations/030_photo_lifecycle.sql
\ir verify.sql
COMMIT;
