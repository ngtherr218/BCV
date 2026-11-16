-- Data only, local v2. Preserve six legacy groups and all user selections.
DO $$ BEGIN
 IF (SELECT count(*) FROM place_type WHERE place_type_id BETWEEN 101 AND 112)<>12 THEN RAISE EXCEPTION 'Missing standard place types'; END IF;
 IF EXISTS(SELECT 1 FROM interest_group WHERE interest_group_id BETWEEN 101 AND 112 AND code<>'PLACE_TYPE_'||interest_group_id) THEN RAISE EXCEPTION 'Interest ID collision'; END IF;
END $$;
INSERT INTO interest_group(interest_group_id,code,name)
SELECT place_type_id,'PLACE_TYPE_'||place_type_id,name FROM place_type WHERE place_type_id BETWEEN 101 AND 112
ON CONFLICT(interest_group_id) DO UPDATE SET name=EXCLUDED.name;
