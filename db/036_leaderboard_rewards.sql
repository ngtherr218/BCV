-- Weekly leaderboard rewards. Each Monday (Vietnam time) the week before is settled once: the top 3 of
-- the Khám phá board and of each level's Vận động and Giữ chuỗi boards get flight tickets. Khám phá and
-- Vận động count that week only; Giữ chuỗi is the longest streak kept so far (it never resets). leaderboard_week marks a settled
-- week; leaderboard_reward keeps who placed where (the "Bảng vàng tuần trước" and the champion mark),
-- what they got, and whether they have seen the reward popup.
CREATE TABLE IF NOT EXISTS leaderboard_week (
    week_start DATE        PRIMARY KEY,
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS leaderboard_reward (
    reward_id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    week_start     DATE        NOT NULL REFERENCES leaderboard_week(week_start),
    scope          VARCHAR(20) NOT NULL CHECK (scope IN ('knowledge', 'activity', 'streak')),
    -- '' for the Khám phá board (everyone together); LOW / MEDIUM / HIGH for Vận động and Giữ chuỗi.
    activity_level VARCHAR(10) NOT NULL DEFAULT '',
    rank           SMALLINT    NOT NULL CHECK (rank BETWEEN 1 AND 3),
    user_id        BIGINT      NOT NULL REFERENCES app_user(user_id) ON DELETE CASCADE,
    value          INTEGER     NOT NULL,
    item_id        BIGINT      REFERENCES item(item_id),
    quantity       INTEGER     NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    seen_at        TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (week_start, scope, activity_level, rank)
);
CREATE INDEX IF NOT EXISTS leaderboard_reward_user_idx ON leaderboard_reward(user_id);

-- Badge metric: how many times the user finished a week in the top 3 ("Lên bục").
ALTER TABLE badge DROP CONSTRAINT IF EXISTS badge_condition_type_check;
ALTER TABLE badge ADD CONSTRAINT badge_condition_type_check CHECK (condition_type IN (
    'PLACES_DISCOVERED', 'ZONES_TOUCHED', 'ZONES_COMPLETED', 'BEST_STREAK', 'REAL_CHECKINS',
    'PROVINCES_STAMPED', 'QUIZ_CORRECT', 'CARDS_MASTERED', 'PODIUM_FINISHES'));
