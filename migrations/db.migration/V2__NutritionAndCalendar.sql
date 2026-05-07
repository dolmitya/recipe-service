ALTER TABLE product
    ADD COLUMN IF NOT EXISTS calories_per_unit NUMERIC(10, 2) NOT NULL DEFAULT 0;

ALTER TABLE recipe
    ADD COLUMN IF NOT EXISTS servings NUMERIC(10, 2) NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS meal_entry
(
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id  BIGINT                  REFERENCES product (id) ON DELETE CASCADE,
    recipe_id   BIGINT                  REFERENCES recipe (id) ON DELETE CASCADE,
    consumed_on DATE           NOT NULL,
    quantity    NUMERIC(10, 3) NOT NULL,
    CONSTRAINT chk_meal_entry_single_source
        CHECK (
            (product_id IS NOT NULL AND recipe_id IS NULL)
                OR
            (product_id IS NULL AND recipe_id IS NOT NULL)
            )
);

CREATE INDEX IF NOT EXISTS idx_meal_entry_user_date ON meal_entry (user_id, consumed_on);
