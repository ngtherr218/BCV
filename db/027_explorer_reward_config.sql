-- Ranking rewards are independent of recommendation model versions.
BEGIN;
CREATE TABLE IF NOT EXISTS public.explorer_reward_config (
    singleton boolean PRIMARY KEY DEFAULT true CHECK(singleton),
    quest_points integer NOT NULL DEFAULT 20 CHECK(quest_points BETWEEN 0 AND 100000),
    journey_points integer NOT NULL DEFAULT 50 CHECK(journey_points BETWEEN 0 AND 100000),
    first_place_points integer NOT NULL DEFAULT 100 CHECK(first_place_points BETWEEN 0 AND 100000),
    revision bigint NOT NULL DEFAULT 1,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by bigint REFERENCES public.app_user(user_id)
);
INSERT INTO public.explorer_reward_config(singleton) VALUES(true) ON CONFLICT DO NOTHING;
COMMIT;
