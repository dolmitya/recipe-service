CREATE TABLE IF NOT EXISTS users
(
    id        BIGSERIAL PRIMARY KEY,
    email     VARCHAR(255) NOT NULL UNIQUE,
    password  VARCHAR(255) NOT NULL,
    full_name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS product
(
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL  UNIQUE,
    unit VARCHAR(32)
);

CREATE TABLE IF NOT EXISTS recipe
(
    id          BIGSERIAL PRIMARY KEY,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(128)
);

CREATE TABLE IF NOT EXISTS ingredient
(
    id         BIGSERIAL PRIMARY KEY,
    recipe_id  BIGINT         NOT NULL REFERENCES recipe (id) ON DELETE CASCADE,
    product_id BIGINT   NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    quantity   NUMERIC(10, 3) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_ingredient_recipe ON ingredient (recipe_id);

CREATE TABLE IF NOT EXISTS favorite_recipe
(
    id        BIGSERIAL PRIMARY KEY,
    user_id   BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    recipe_id BIGINT NOT NULL REFERENCES recipe (id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_favorite_user ON favorite_recipe (user_id);
CREATE INDEX IF NOT EXISTS idx_favorite_recipe ON favorite_recipe (recipe_id);

CREATE TABLE IF NOT EXISTS users_product
(
    id         BIGSERIAL PRIMARY KEY,
    user_id  BIGINT         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    product_id BIGINT   NOT NULL REFERENCES product (id) ON DELETE CASCADE,
    quantity   NUMERIC(10, 3) NOT NULL
);