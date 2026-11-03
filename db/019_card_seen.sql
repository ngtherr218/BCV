-- Existing cards are seen; newly earned cards start unseen.
BEGIN;
ALTER TABLE public.user_card ADD COLUMN IF NOT EXISTS seen_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE public.user_card ALTER COLUMN seen_at DROP DEFAULT;
COMMIT;
