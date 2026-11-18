\set ON_ERROR_STOP on
\encoding UTF8
BEGIN;
SELECT pg_advisory_xact_lock(782341905);
\ir migrations/031_culture_quiz.sql
\ir seed/004_place_questions.sql
\ir verify.sql
COMMIT;
