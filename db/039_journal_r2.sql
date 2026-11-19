ALTER TABLE journal_photo ADD COLUMN IF NOT EXISTS object_key TEXT;
ALTER TABLE journal_photo ALTER COLUMN photo_data DROP NOT NULL;
ALTER TABLE journal_photo ADD CONSTRAINT journal_photo_storage_check
    CHECK ((photo_data IS NOT NULL) <> (object_key IS NOT NULL));
