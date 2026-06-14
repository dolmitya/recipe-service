CREATE TABLE IF NOT EXISTS recipe_category
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(128) NOT NULL UNIQUE
);

INSERT INTO recipe_category (name)
SELECT DISTINCT LOWER(TRIM(category))
FROM recipe
WHERE category IS NOT NULL
  AND TRIM(category) <> ''
ON CONFLICT (name) DO NOTHING;

ALTER TABLE recipe
    ADD COLUMN IF NOT EXISTS category_id BIGINT REFERENCES recipe_category (id);

UPDATE recipe r
SET category_id = rc.id
FROM recipe_category rc
WHERE r.category IS NOT NULL
  AND LOWER(TRIM(r.category)) = rc.name
  AND r.category_id IS NULL;

CREATE INDEX IF NOT EXISTS idx_recipe_category ON recipe (category_id);

ALTER TABLE recipe
    DROP COLUMN IF EXISTS category;
