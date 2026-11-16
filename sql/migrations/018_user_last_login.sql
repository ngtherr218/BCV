-- Existing accounts have no reliable historical login timestamp; do not backfill invented times.
ALTER TABLE public.app_user ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMPTZ NULL;
