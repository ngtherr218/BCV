BEGIN;
ALTER TABLE public.explorer_reward_config ADD COLUMN IF NOT EXISTS discovery_points integer;
UPDATE public.explorer_reward_config SET discovery_points=journey_points+first_place_points WHERE discovery_points IS NULL;
ALTER TABLE public.explorer_reward_config ALTER COLUMN discovery_points SET DEFAULT 150,
    ALTER COLUMN discovery_points SET NOT NULL;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.explorer_reward_config'::regclass AND conname='reward_discovery_range') THEN
  ALTER TABLE public.explorer_reward_config ADD CONSTRAINT reward_discovery_range CHECK(discovery_points BETWEEN 0 AND 200000);
 END IF;
END $$;
ALTER TABLE public.quest ADD COLUMN IF NOT EXISTS reward_points integer;
UPDATE public.quest SET reward_points=COALESCE((SELECT quest_points FROM public.explorer_reward_config WHERE singleton),20) WHERE reward_points IS NULL;
ALTER TABLE public.quest ALTER COLUMN reward_points SET DEFAULT 20, ALTER COLUMN reward_points SET NOT NULL;
ALTER TABLE public.quest ADD COLUMN IF NOT EXISTS description text;
DO $$ BEGIN
 IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conrelid='public.quest'::regclass AND conname='quest_reward_points_range') THEN
  ALTER TABLE public.quest ADD CONSTRAINT quest_reward_points_range CHECK(reward_points BETWEEN 0 AND 100000);
 END IF;
END $$;
ALTER TABLE public.quest_progress ADD COLUMN IF NOT EXISTS awarded_points integer NOT NULL DEFAULT 0;
ALTER TABLE public.quest_progress ADD COLUMN IF NOT EXISTS points_settled boolean NOT NULL DEFAULT false;
-- Snapshot existing completions without creating or changing any ledger event.
UPDATE public.quest_progress qp SET awarded_points=COALESCE((SELECT e.points FROM public.explorer_score_event e
 WHERE e.user_id=qp.user_id AND e.event_type='QUEST' AND e.source_key=qp.quest_progress_id::text),0),points_settled=true
 WHERE qp.completion_status AND NOT qp.points_settled;
DO $$ BEGIN
 ALTER TABLE public.explorer_score_event DROP CONSTRAINT IF EXISTS explorer_score_event_event_type_check;
 ALTER TABLE public.explorer_score_event DROP CONSTRAINT IF EXISTS explorer_score_event_type_check;
 ALTER TABLE public.explorer_score_event ADD CONSTRAINT explorer_score_event_type_check
 CHECK(event_type IN ('FIRST_PLACE','JOURNEY','QUEST','DISCOVERY'));
END $$;
COMMIT;
