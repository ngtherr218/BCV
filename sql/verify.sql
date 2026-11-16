\set ON_ERROR_STOP on
\encoding UTF8
DO $$
DECLARE t text; pair text[];
BEGIN
 FOREACH t IN ARRAY ARRAY['app_user','card','collection','collection_card','community_media','explorer_reward_config','explorer_score_event','interest_group','item','journey','media_report','notification','place','place_feature','place_feature_value','place_image','place_type','place_type_feature','quest','quest_progress','recommendation_config','recommendation_criterion','recommendation_rating_snapshot','recommendation_weight','region','step_data','user_card','user_discovered_place','user_item','user_preference','user_preference_interest','user_preference_place_type','virtual_location'] LOOP
  IF to_regclass('public.'||t) IS NULL THEN RAISE EXCEPTION 'Missing table %',t; END IF;
 END LOOP;
 FOREACH pair SLICE 1 IN ARRAY ARRAY[['app_user','last_login_at'],['app_user','media_upload_blocked'],['user_card','seen_at'],['notification','hidden_at'],['place','feature_source'],['recommendation_config','settings'],['quest','reward_points'],['quest','description'],['quest_progress','awarded_points'],['quest_progress','points_settled'],['explorer_reward_config','discovery_points'],['community_media','file_deleted_at'],['community_media','delete_after']] LOOP
  IF NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema='public' AND table_name=pair[1] AND column_name=pair[2]) THEN
   RAISE EXCEPTION 'Missing column %.%',pair[1],pair[2];
  END IF;
 END LOOP;
 IF NOT EXISTS(SELECT 1 FROM pg_extension WHERE extname='postgis') THEN RAISE EXCEPTION 'PostGIS missing'; END IF;
 IF (SELECT count(*) FROM public.explorer_reward_config WHERE singleton)<>1 THEN RAISE EXCEPTION 'Reward configuration missing'; END IF;
 IF (SELECT count(*) FROM public.interest_group WHERE interest_group_id BETWEEN 101 AND 112)<>12 THEN RAISE EXCEPTION 'Twelve standard interests missing'; END IF;
 IF (SELECT count(*) FROM public.recommendation_config WHERE status='ACTIVE' AND settings IS NOT NULL)<>1 THEN RAISE EXCEPTION 'Expected one active recommendation model with metadata'; END IF;
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.explorer_score_event'::regclass AND contype='c' AND pg_get_constraintdef(oid) LIKE '%DISCOVERY%') THEN RAISE EXCEPTION 'Discovery score event is unsupported'; END IF;
END $$;
SELECT 'BCV v2 schema verified' AS result, public.postgis_version() AS postgis;
SELECT count(*) AS places FROM public.place;
SELECT count(*) AS accounts FROM public.app_user;
SELECT discovery_points,revision FROM public.explorer_reward_config;
