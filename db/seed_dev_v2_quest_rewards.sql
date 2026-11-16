BEGIN;
DO $$ BEGIN IF current_database()<>'vietnam_explorer_dev_v2' THEN RAISE EXCEPTION 'Wrong database'; END IF; END $$;
SELECT pg_advisory_xact_lock(28062026);
INSERT INTO quest(name,description,quest_type,cycle_type,condition_type,target_value,reward_points,status)
SELECT v.name,'Nhiệm vụ mẫu với mức điểm có thể điều chỉnh trong trang quản trị.',v.cycle,v.cycle,v.condition,v.target,v.points,'ACTIVE'
FROM (VALUES ('Khởi động ngày mới','DAILY','STEP_COUNT',1000,10),
 ('Đi bộ năng động','DAILY','STEP_COUNT',3000,25),
 ('Chinh phục 8.000 bước','DAILY','STEP_COUNT',8000,60),
 ('Khám phá địa danh','DAILY','DISCOVER_PLACE',1,30),
 ('Kiên trì vận động','WEEKLY','STEP_COUNT',30000,150),
 ('Nhà khám phá','WEEKLY','DISCOVER_PLACE',3,100),
 ('Chinh phục hành trình','WEEKLY','COMPLETE_JOURNEY',5,180)) v(name,cycle,condition,target,points)
WHERE NOT EXISTS(SELECT 1 FROM quest q WHERE q.name=v.name);
COMMIT;
