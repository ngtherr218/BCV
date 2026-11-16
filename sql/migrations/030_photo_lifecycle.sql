-- Preserve moderation metadata and stop on unsupported legacy videos instead of deleting them.
DO $$ BEGIN
 IF EXISTS(SELECT 1 FROM public.community_media WHERE mime_type NOT IN ('image/jpeg','image/png','image/webp')) THEN
  RAISE EXCEPTION 'Archive unsupported community media through a reviewed migration before deploying the photo-only API';
 END IF;
END $$;
ALTER TABLE public.community_media ALTER COLUMN media_data DROP NOT NULL;
ALTER TABLE public.community_media ADD COLUMN IF NOT EXISTS file_deleted_at timestamptz;
ALTER TABLE public.community_media ADD COLUMN IF NOT EXISTS delete_after timestamptz;
ALTER TABLE public.community_media DROP CONSTRAINT IF EXISTS community_media_photo_mime_check;
ALTER TABLE public.community_media ADD CONSTRAINT community_media_photo_mime_check
 CHECK(mime_type IN ('image/jpeg','image/png','image/webp'));
ALTER TABLE public.media_report DROP CONSTRAINT IF EXISTS media_report_status_check;
ALTER TABLE public.media_report ADD CONSTRAINT media_report_status_check
 CHECK(status IN ('OPEN','RESOLVED','ACCEPTED','DISMISSED'));
