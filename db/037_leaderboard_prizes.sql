-- What rank 1, 2 and 3 of every weekly board win, set on the admin web (Trò chơi hóa › Giải thưởng tuần).
-- One row per rank, the same for Khám phá, Vận động and Giữ chuỗi. No item (or quantity 0) = no prize.
-- A change applies from the next settled week; prizes already given stay as they were.
CREATE TABLE IF NOT EXISTS leaderboard_prize (
    rank       BIGINT       PRIMARY KEY CHECK (rank BETWEEN 1 AND 3),
    title      VARCHAR(20)  NOT NULL,
    item_id    BIGINT       REFERENCES item(item_id),
    quantity   INTEGER      NOT NULL DEFAULT 0 CHECK (quantity BETWEEN 0 AND 100),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

INSERT INTO leaderboard_prize (rank, title, item_id, quantity)
SELECT r.rank, 'Hạng ' || r.rank,
       (SELECT item_id FROM item WHERE item_type='FLIGHT_TICKET' AND status='ACTIVE' ORDER BY item_id LIMIT 1), r.quantity
FROM (VALUES (1, 5), (2, 3), (3, 2)) AS r(rank, quantity)
ON CONFLICT (rank) DO NOTHING;
