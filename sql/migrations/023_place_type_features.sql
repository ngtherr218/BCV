-- Existing curated places retain their own features. New places inherit explicitly.
DO $migration$
BEGIN
 IF to_regclass('place_type_feature') IS NULL THEN
  CREATE TABLE place_type_feature (place_type_id bigint NOT NULL REFERENCES place_type ON DELETE CASCADE, code varchar(50) NOT NULL, PRIMARY KEY(place_type_id,code));
  INSERT INTO place_type_feature SELECT v.id,v.code FROM (VALUES
(101,'kind__heritage'),
(101,'theme__history'),
(101,'theme__culture'),
(102,'kind__religious'),
(102,'theme__spirituality'),
(103,'kind__structure'),
(103,'theme__architecture'),
(104,'kind__community'),
(104,'theme__culture'),
(105,'kind__market'),
(105,'theme__cuisine'),
(106,'kind__recreation'),
(106,'theme__relaxation'),
(107,'kind__agriculture'),
(107,'theme__agriculture'),
(108,'kind__nature'),
(108,'landform__coast'),
(109,'kind__nature'),
(109,'landform__river'),
(110,'kind__nature'),
(110,'landform__mountain'),
(111,'kind__nature'),
(111,'landform__cave'),
(111,'theme__geology'),
(112,'kind__nature'),
(112,'ecosystem__forest'),
(112,'theme__ecology')
  ) v(id,code) JOIN place_type t ON t.place_type_id=v.id;
 END IF;
END $migration$;
ALTER TABLE place ADD COLUMN IF NOT EXISTS feature_source varchar(10) NOT NULL DEFAULT 'CUSTOM' CHECK(feature_source IN ('TYPE','CUSTOM'));
