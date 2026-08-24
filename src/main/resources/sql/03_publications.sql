CREATE TABLE IF NOT EXISTS "categories"
(
    "id"   SERIAL PRIMARY KEY NOT NULL,
    "name" VARCHAR(50)        NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS "products"
(
    "id"          UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "name"        VARCHAR(70)      NOT NULL,
    "quantity"    NUMERIC(10, 2)   NOT NULL DEFAULT 0.00,
    "sale_price"  NUMERIC(10, 2)   NOT NULL DEFAULT 0.00,
    "category_id" INTEGER          NOT NULL REFERENCES categories (id) ON DELETE SET NULL,
    "user_id"     UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    "sale_type"   VARCHAR(30)      NOT NULL,
    "photo_url"   VARCHAR(300)     NOT NULL,
    "created_at"  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "updated_at"  TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "publications"
(
    "id"          UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "title"       VARCHAR(120)     NOT NULL,
    "description" VARCHAR(350)     NOT NULL,
    "created_at"  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "posted_at"   TIMESTAMPTZ,
    "user_id"     UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    "hidden"      BOOLEAN          NOT NULL DEFAULT FALSE
);

CREATE TABLE IF NOT EXISTS "publication_media"
(
    "id"             UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "media_type"     VARCHAR(20)      NOT NULL,
    "media_url"      VARCHAR(300)     NOT NULL,
    "display_order"  INTEGER          NOT NULL,
    "publication_id" UUID             NOT NULL REFERENCES publications (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "product_publication"
(
    "product_id"     UUID NOT NULL REFERENCES products (id) ON DELETE CASCADE,
    "publication_id" UUID NOT NULL REFERENCES publications (id) ON DELETE CASCADE,
    PRIMARY KEY ("product_id", "publication_id")
);

CREATE TABLE IF NOT EXISTS "orders"
(
    "id"                UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "user_id"           UUID             NOT NULL REFERENCES users (id) ON DELETE SET NULL,
    "created_at"        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "updated_at"        TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "payment_method"    VARCHAR(20)      NOT NULL,
    "amount_paid"       NUMERIC(10, 2),
    "payment_proof_url" VARCHAR(300),
    "status"            VARCHAR(30)      NOT NULL
);

CREATE TABLE IF NOT EXISTS "product_order"
(
    "product_id" UUID           NOT NULL DEFAULT gen_random_uuid(),
    "order_id"   UUID           NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    "quantity"   NUMERIC(10, 2) NOT NULL,
    "unit_price" NUMERIC(10, 2) NOT NULL,
    PRIMARY KEY ("product_id", "order_id")
);

CREATE TABLE IF NOT EXISTS "tags"
(
    "id"   SERIAL PRIMARY KEY NOT NULL,
    "name" VARCHAR(30)        NOT NULL
);

CREATE TABLE IF NOT EXISTS "tag_publication"
(
    "tag_id"         INTEGER NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    "publication_id" UUID    NOT NULL REFERENCES publications (id) ON DELETE CASCADE,
    PRIMARY KEY ("tag_id", "publication_id")
);
