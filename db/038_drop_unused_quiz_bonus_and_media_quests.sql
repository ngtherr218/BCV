-- Clean-up of two leftovers.
-- 1) bonus_steps: culture questions once added bonus distance to the journey; they now give explorer
--    points only (10/20/30 by difficulty), so the per-journey and per-answer bonus columns are unused.
-- 2) Quest conditions MEDIA_APPROVED / PHOTO_APPROVED belonged to the retired community uploads (029):
--    nothing counts toward them any more. Quests that still use one are switched off, and the condition
--    list is pinned to the four that work.
ALTER TABLE journey DROP COLUMN IF EXISTS bonus_steps;
ALTER TABLE journey_quiz_answer DROP COLUMN IF EXISTS bonus_steps;

UPDATE quest SET status = 'INACTIVE'
WHERE condition_type IN ('MEDIA_APPROVED', 'PHOTO_APPROVED') AND status <> 'INACTIVE';
-- NOT VALID: such a switched-off quest keeps its history; every new or edited quest must use one of the four.
ALTER TABLE quest DROP CONSTRAINT IF EXISTS quest_condition_type_check;
ALTER TABLE quest ADD CONSTRAINT quest_condition_type_check CHECK (
    condition_type IN ('STEP_COUNT', 'DISCOVER_PLACE', 'COMPLETE_JOURNEY', 'QUIZ_CORRECT')
) NOT VALID;
