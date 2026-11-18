-- F81: travel zones for the passport. New regions stay NULL until an admin assigns them.
ALTER TABLE region ADD COLUMN IF NOT EXISTS zone VARCHAR(30);
UPDATE region r SET zone = z.zone FROM (VALUES
    ('Lai Châu','TAY_BAC'),('Điện Biên','TAY_BAC'),('Sơn La','TAY_BAC'),('Hòa Bình','TAY_BAC'),('Lào Cai','TAY_BAC'),('Yên Bái','TAY_BAC'),
    ('Hà Giang','DONG_BAC'),('Cao Bằng','DONG_BAC'),('Bắc Kạn','DONG_BAC'),('Lạng Sơn','DONG_BAC'),('Tuyên Quang','DONG_BAC'),
    ('Thái Nguyên','DONG_BAC'),('Phú Thọ','DONG_BAC'),('Bắc Giang','DONG_BAC'),('Quảng Ninh','DONG_BAC'),
    ('Hà Nội','SONG_HONG'),('Vĩnh Phúc','SONG_HONG'),('Bắc Ninh','SONG_HONG'),('Hải Dương','SONG_HONG'),('Hải Phòng','SONG_HONG'),
    ('Hưng Yên','SONG_HONG'),('Thái Bình','SONG_HONG'),('Hà Nam','SONG_HONG'),('Nam Định','SONG_HONG'),('Ninh Bình','SONG_HONG'),
    ('Thanh Hóa','BAC_TRUNG_BO'),('Nghệ An','BAC_TRUNG_BO'),('Hà Tĩnh','BAC_TRUNG_BO'),('Quảng Bình','BAC_TRUNG_BO'),
    ('Quảng Trị','BAC_TRUNG_BO'),('Thừa Thiên Huế','BAC_TRUNG_BO'),
    ('Đà Nẵng','NAM_TRUNG_BO'),('Quảng Nam','NAM_TRUNG_BO'),('Quảng Ngãi','NAM_TRUNG_BO'),('Bình Định','NAM_TRUNG_BO'),
    ('Phú Yên','NAM_TRUNG_BO'),('Khánh Hòa','NAM_TRUNG_BO'),('Ninh Thuận','NAM_TRUNG_BO'),('Bình Thuận','NAM_TRUNG_BO'),
    ('Kon Tum','TAY_NGUYEN'),('Gia Lai','TAY_NGUYEN'),('Đắk Lắk','TAY_NGUYEN'),('Đắk Nông','TAY_NGUYEN'),('Lâm Đồng','TAY_NGUYEN'),
    ('Thành phố Hồ Chí Minh','DONG_NAM_BO'),('Bình Dương','DONG_NAM_BO'),('Bình Phước','DONG_NAM_BO'),('Tây Ninh','DONG_NAM_BO'),
    ('Đồng Nai','DONG_NAM_BO'),('Bà Rịa - Vũng Tàu','DONG_NAM_BO'),
    ('Long An','TAY_NAM_BO'),('Tiền Giang','TAY_NAM_BO'),('Bến Tre','TAY_NAM_BO'),('Trà Vinh','TAY_NAM_BO'),('Vĩnh Long','TAY_NAM_BO'),
    ('Đồng Tháp','TAY_NAM_BO'),('An Giang','TAY_NAM_BO'),('Kiên Giang','TAY_NAM_BO'),('Cần Thơ','TAY_NAM_BO'),('Hậu Giang','TAY_NAM_BO'),
    ('Sóc Trăng','TAY_NAM_BO'),('Bạc Liêu','TAY_NAM_BO'),('Cà Mau','TAY_NAM_BO')
) AS z(name, zone)
WHERE r.name = z.name AND r.zone IS NULL;

-- Khu vực = the 34 provinces and cities since the 2025 merger (Nghị quyết 202/2025/QH15).
-- The seed data has the 63 former provinces; each group is folded into one region: the one already
-- named like the new province (else the lowest id) keeps its id and takes the new name and Vùng;
-- the others' places and virtual locations move onto it, then they are deleted. Re-runnable: once
-- merged, no former province is left to fold. A database that still has the old province table
-- (and region.province_id) gets both removed.
CREATE TEMP TABLE province34 (province_id SMALLINT, name TEXT, area TEXT) ON COMMIT DROP;
INSERT INTO province34 (province_id, name, area) VALUES
    (1,'Lai Châu','MIEN_NUI_BAC'),(2,'Điện Biên','MIEN_NUI_BAC'),(3,'Sơn La','MIEN_NUI_BAC'),(4,'Lào Cai','MIEN_NUI_BAC'),
    (5,'Tuyên Quang','MIEN_NUI_BAC'),(6,'Cao Bằng','MIEN_NUI_BAC'),(7,'Lạng Sơn','MIEN_NUI_BAC'),(8,'Thái Nguyên','MIEN_NUI_BAC'),
    (9,'Phú Thọ','MIEN_NUI_BAC'),
    (10,'Hà Nội','SONG_HONG'),(11,'Hải Phòng','SONG_HONG'),(12,'Quảng Ninh','SONG_HONG'),(13,'Bắc Ninh','SONG_HONG'),
    (14,'Hưng Yên','SONG_HONG'),(15,'Ninh Bình','SONG_HONG'),
    (16,'Thanh Hóa','MIEN_TRUNG'),(17,'Nghệ An','MIEN_TRUNG'),(18,'Hà Tĩnh','MIEN_TRUNG'),(19,'Quảng Trị','MIEN_TRUNG'),
    (20,'Huế','MIEN_TRUNG'),(21,'Đà Nẵng','MIEN_TRUNG'),(22,'Quảng Ngãi','MIEN_TRUNG'),(23,'Khánh Hòa','MIEN_TRUNG'),
    (24,'Gia Lai','TAY_NGUYEN'),(25,'Đắk Lắk','TAY_NGUYEN'),(26,'Lâm Đồng','TAY_NGUYEN'),
    (27,'TP. Hồ Chí Minh','DONG_NAM_BO'),(28,'Đồng Nai','DONG_NAM_BO'),(29,'Tây Ninh','DONG_NAM_BO'),
    (30,'Cần Thơ','TAY_NAM_BO'),(31,'Vĩnh Long','TAY_NAM_BO'),(32,'Đồng Tháp','TAY_NAM_BO'),(33,'Cà Mau','TAY_NAM_BO'),
    (34,'An Giang','TAY_NAM_BO');
CREATE TEMP TABLE former_province (name TEXT, province_id SMALLINT) ON COMMIT DROP;
INSERT INTO former_province (name, province_id) VALUES
    ('Lai Châu',1),('Điện Biên',2),('Sơn La',3),('Lào Cai',4),('Yên Bái',4),('Tuyên Quang',5),('Hà Giang',5),
    ('Cao Bằng',6),('Lạng Sơn',7),('Thái Nguyên',8),('Bắc Kạn',8),('Phú Thọ',9),('Vĩnh Phúc',9),('Hòa Bình',9),
    ('Hà Nội',10),('Hải Phòng',11),('Hải Dương',11),('Quảng Ninh',12),('Bắc Ninh',13),('Bắc Giang',13),
    ('Hưng Yên',14),('Thái Bình',14),('Ninh Bình',15),('Hà Nam',15),('Nam Định',15),
    ('Thanh Hóa',16),('Nghệ An',17),('Hà Tĩnh',18),('Quảng Trị',19),('Quảng Bình',19),('Thừa Thiên Huế',20),
    ('Đà Nẵng',21),('Quảng Nam',21),('Quảng Ngãi',22),('Kon Tum',22),('Khánh Hòa',23),('Ninh Thuận',23),
    ('Gia Lai',24),('Bình Định',24),('Đắk Lắk',25),('Phú Yên',25),('Lâm Đồng',26),('Đắk Nông',26),('Bình Thuận',26),
    ('Thành phố Hồ Chí Minh',27),('Bình Dương',27),('Bà Rịa - Vũng Tàu',27),('Đồng Nai',28),('Bình Phước',28),
    ('Tây Ninh',29),('Long An',29),('Cần Thơ',30),('Sóc Trăng',30),('Hậu Giang',30),
    ('Vĩnh Long',31),('Bến Tre',31),('Trà Vinh',31),('Đồng Tháp',32),('Tiền Giang',32),('Cà Mau',33),('Bạc Liêu',33),
    ('An Giang',34),('Kiên Giang',34);

CREATE TEMP TABLE region_target ON COMMIT DROP AS
    SELECT r.region_id, f.province_id FROM region r
    JOIN former_province f ON normalize(r.name, NFC) = normalize(f.name, NFC);
CREATE TEMP TABLE region_keeper ON COMMIT DROP AS
    SELECT DISTINCT ON (t.province_id) t.province_id, t.region_id
    FROM region_target t JOIN region r ON r.region_id = t.region_id JOIN province34 p ON p.province_id = t.province_id
    ORDER BY t.province_id, (normalize(r.name, NFC) = normalize(p.name, NFC)) DESC, t.region_id;
CREATE TEMP TABLE region_folded ON COMMIT DROP AS
    SELECT t.region_id AS old_id, k.region_id AS keeper_id
    FROM region_target t JOIN region_keeper k ON k.province_id = t.province_id
    WHERE t.region_id <> k.region_id;

UPDATE place p SET region_id = f.keeper_id FROM region_folded f WHERE p.region_id = f.old_id;
UPDATE virtual_location v SET region_id = f.keeper_id FROM region_folded f WHERE v.region_id = f.old_id;
-- a merged province stays active when any of its former provinces was
UPDATE region r SET status = 'ACTIVE'
FROM region_folded f JOIN region o ON o.region_id = f.old_id
WHERE r.region_id = f.keeper_id AND o.status = 'ACTIVE' AND r.status <> 'ACTIVE';
DELETE FROM region r USING region_folded f WHERE r.region_id = f.old_id;
UPDATE region r SET name = p.name, zone = p.area
FROM region_keeper k JOIN province34 p ON p.province_id = k.province_id
WHERE r.region_id = k.region_id
  AND (normalize(r.name, NFC) <> normalize(p.name, NFC)
       OR r.zone IS NULL OR r.zone NOT IN ('MIEN_NUI_BAC','SONG_HONG','MIEN_TRUNG','TAY_NGUYEN','DONG_NAM_BO','TAY_NAM_BO'));

ALTER TABLE region DROP COLUMN IF EXISTS province_id;
DROP TABLE IF EXISTS province;
