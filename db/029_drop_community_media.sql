-- Community photo/video uploads are retired: photos now live in each user's private journal and
-- place videos are managed by administrators (place_video). Re-runnable.
DROP TABLE IF EXISTS media_report;
DROP TABLE IF EXISTS community_media;
ALTER TABLE app_user DROP COLUMN IF EXISTS media_upload_blocked;
