-- Hiding notices preserves their event history.
ALTER TABLE public.notification ADD COLUMN IF NOT EXISTS hidden_at TIMESTAMPTZ NULL;
