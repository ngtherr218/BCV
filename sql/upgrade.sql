\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;
SELECT pg_advisory_xact_lock(782341905);
SET LOCAL search_path TO public;
DO $$ BEGIN
 IF to_regclass('public.app_user') IS NULL OR to_regclass('public.recommendation_config') IS NULL THEN
  RAISE EXCEPTION 'Expected the baseline BCV application schema. Use install.sql for a fresh database';
 END IF;
 IF (SELECT count(*) FROM public.place_type WHERE place_type_id BETWEEN 101 AND 112)<>12 THEN
  RAISE EXCEPTION 'Expected standard place types 101-112 (historical migration 014). Review sql/README.md before upgrading';
 END IF;
 IF (SELECT count(*) FROM public.recommendation_config WHERE status='ACTIVE')<>1 THEN
  RAISE EXCEPTION 'Expected exactly one reviewed active recommendation model before upgrading';
 END IF;
END $$;
\ir migrations/018_user_last_login.sql
\ir migrations/019_card_seen.sql
\ir migrations/020_notification_hidden.sql
\ir migrations/021_versioned_recommendations.sql
\ir migrations/022_community_media.sql
\ir migrations/023_place_type_features.sql
\ir migrations/024_media_moderation.sql
\ir migrations/026_score_ledger.sql
\ir migrations/027_explorer_reward_config.sql
\ir migrations/028_unified_discovery_quest_points.sql
\ir migrations/029_twelve_interests.sql
\ir migrations/030_photo_lifecycle.sql
\ir verify.sql
COMMIT;
