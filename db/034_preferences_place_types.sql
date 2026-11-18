-- Preferences are favourite place types (user_preference_place_type), as in bcv_v2. Choices made on
-- the former 6 interest groups move to the matching place types, keeping at most 3 per user;
-- then interest_group and user_preference_interest are dropped. Re-runnable.
DO $$
BEGIN
    IF to_regclass('user_preference_interest') IS NOT NULL THEN
        DELETE FROM user_preference_place_type t
        WHERE EXISTS (SELECT 1 FROM user_preference_interest i WHERE i.user_preference_id = t.user_preference_id);
        INSERT INTO user_preference_place_type (user_preference_id, place_type_id)
        SELECT DISTINCT i.user_preference_id, m.place_type_id
        FROM user_preference_interest i
        JOIN (VALUES (201, 101), (205, 101), (206, 103), (203, 105), (204, 108), (202, 110), (202, 109))
             AS m(interest_group_id, place_type_id) ON m.interest_group_id = i.interest_group_id
        JOIN place_type pt ON pt.place_type_id = m.place_type_id
        ON CONFLICT DO NOTHING;
        DELETE FROM user_preference_place_type t USING (
            SELECT user_preference_id, place_type_id,
                   row_number() OVER (PARTITION BY user_preference_id ORDER BY place_type_id) AS n
            FROM user_preference_place_type) r
        WHERE r.user_preference_id = t.user_preference_id AND r.place_type_id = t.place_type_id AND r.n > 3;
        DROP TABLE user_preference_interest;
    END IF;
END $$;
DROP TABLE IF EXISTS interest_group;
