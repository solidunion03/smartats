ALTER TABLE scan ADD COLUMN skill_score     INTEGER;
ALTER TABLE scan ADD COLUMN semantic_score  INTEGER;
ALTER TABLE scan ADD COLUMN matched_skills  TEXT;
ALTER TABLE scan ADD COLUMN missing_skills  TEXT;