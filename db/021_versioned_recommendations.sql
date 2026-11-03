-- Apply before deploying the six-interest backend. Legacy selections are preserved;
-- users choose the new independent groups explicitly at their next sign-in.
BEGIN;
CREATE TABLE IF NOT EXISTS interest_group (
 interest_group_id bigint PRIMARY KEY, code varchar(50) NOT NULL UNIQUE, name varchar(100) NOT NULL
);
INSERT INTO interest_group VALUES
 (201,'PREF_HERITAGE','Di sản'),(202,'PREF_NATURE','Thiên nhiên'),
 (203,'PREF_CUISINE','Ẩm thực'),(204,'PREF_COAST_ISLAND','Biển đảo'),
 (205,'PREF_HISTORY','Lịch sử'),(206,'PREF_ARCHITECTURE','Kiến trúc')
ON CONFLICT DO NOTHING;
CREATE TABLE IF NOT EXISTS user_preference_interest (
 user_preference_id bigint NOT NULL REFERENCES user_preference(user_preference_id) ON DELETE CASCADE,
 interest_group_id bigint NOT NULL REFERENCES interest_group(interest_group_id),
 PRIMARY KEY(user_preference_id,interest_group_id)
);
ALTER TABLE recommendation_config ADD COLUMN IF NOT EXISTS settings jsonb;
DO $guard$
BEGIN
 IF EXISTS(SELECT 1 FROM recommendation_config WHERE settings IS NULL AND version <> 'rank_20261020_draft') THEN
  RAISE EXCEPTION 'Review metadata for custom model versions before applying this migration';
 END IF;
END
$guard$;
UPDATE recommendation_config SET settings = '{
 "datasetVersion":"canonical-311-20261020",
 "modelVersion":"kmeans-k15-seed42",
 "modelSha256":"0f79c0880d04dca5d056354689a33b1ec93cf203a11335cefc550ce1cc81f3d3",
 "mappingVersion":"six-interests-v1",
 "activityThresholds":{"LOW":3000,"MEDIUM":6000,"HIGH":10000},
 "interestFeatures":{"201":["kind__heritage"],"202":["kind__nature"],
 "203":["theme__cuisine"],"204":["landform__coast","landform__island"],
 "205":["theme__history"],"206":["theme__architecture"]}
}'::jsonb, version=version || '_six_v1' WHERE settings IS NULL;
ALTER TABLE recommendation_config ALTER COLUMN settings SET NOT NULL;
-- Freeze rating/count for each model version: admin edits take effect after a new import.
CREATE TABLE IF NOT EXISTS recommendation_rating_snapshot (
 recommendation_config_id bigint NOT NULL REFERENCES recommendation_config,
 place_id bigint NOT NULL REFERENCES place,
 rating numeric(3,2), rating_count integer,
 PRIMARY KEY(recommendation_config_id,place_id),
 CHECK(rating IS NULL OR rating BETWEEN 0 AND 5),
 CHECK(rating_count IS NULL OR rating_count>=0)
);
INSERT INTO recommendation_rating_snapshot
SELECT pf.recommendation_config_id,p.place_id,p.rating,p.rating_count
FROM place_feature pf JOIN place p USING(place_id) ON CONFLICT DO NOTHING;
CREATE UNIQUE INDEX IF NOT EXISTS one_active_recommendation_config
 ON recommendation_config ((status)) WHERE status='ACTIVE';
COMMIT;
