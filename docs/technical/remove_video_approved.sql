-- Explicitly requested video removal; local v2 only. No CASCADE table drop.
BEGIN;
DO $$ BEGIN
 IF current_database()<>'vietnam_explorer_dev_v2' THEN RAISE EXCEPTION 'Wrong database'; END IF;
END $$;
DELETE FROM public.community_media WHERE mime_type LIKE 'video/%';
DROP TABLE IF EXISTS public.place_video;
COMMIT;
