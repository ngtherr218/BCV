-- Reference catalog only. No ownership, discovery or steps are granted by this seed.
BEGIN;
DO $$ BEGIN
 IF current_database()<>'vietnam_explorer_dev_v2' THEN RAISE EXCEPTION 'Only local v2 is allowed'; END IF;
END $$;
UPDATE card c SET name=v.name,description=v.description,status='ACTIVE',
 image_key=COALESCE(c.image_key,(SELECT image_key FROM place_image i WHERE i.place_id=c.place_id ORDER BY place_image_id LIMIT 1))
FROM (VALUES
 (2::bigint,'Tháp Chăm Ponagar','Dấu ấn kiến trúc Chăm tại Tháp Bà Ponagar.'),
 (6,'Ngọc đảo Phú Quốc','Thẻ khám phá đảo ngọc và văn hóa biển Phú Quốc.'),
 (11,'Nhà gỗ Tây Nguyên','Dấu ấn Nhà Thờ Gỗ Kon Tum và kiến trúc gỗ địa phương.'),
 (31,'Cờ trên mây Fansipan','Kỷ niệm hành trình khám phá đỉnh Fansipan.'),
 (51,'Thuyền khám phá Tràng An','Thuyền khám phá vùng núi đá vôi và di sản Tràng An.'),
 (71,'Thạch nhũ Phong Nha','Dấu ấn hang động và cảnh quan địa chất Phong Nha.'),
 (76,'Đèn lồng Hội An','Biểu tượng đèn lồng gắn với không gian văn hóa phố cổ Hội An.'),
 (86,'Thuyền buồm Hạ Long','Thẻ văn hóa biển và cảnh quan vịnh Hạ Long.'),
 (283,'Khuê Văn Các','Dấu ấn truyền thống hiếu học tại Văn Miếu – Quốc Tử Giám.'),
 (21,'Sắc hoa Đà Lạt','Kỷ niệm khám phá Hồ Xuân Hương và cảnh quan thành phố Đà Lạt.')
) v(place_id,name,description) WHERE c.place_id=v.place_id;
DO $$ BEGIN
 IF (SELECT count(*) FROM card WHERE place_id IN (2,6,11,31,51,71,76,86,283,21) AND image_key IS NOT NULL AND status='ACTIVE')<>10 THEN
   RAISE EXCEPTION 'Ten cultural cards require real reference cover images';
 END IF;
END $$;
COMMIT;
