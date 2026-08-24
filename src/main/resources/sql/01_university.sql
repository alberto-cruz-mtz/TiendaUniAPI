CREATE TABLE IF NOT EXISTS "universities"
(
    "id"          UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "name"        VARCHAR(140)     NOT NULL,
    "acronym"     VARCHAR(15)      NOT NULL,
    "logo_url"    VARCHAR(300)     NOT NULL,
    -- podria ser oklch/rgb/hexadecimal
    "brand_color" VARCHAR(30)      NOT NULL DEFAULT '#000000',
    "created_at"  TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "updated_at"  TIMESTAMPTZ      NOT NULL DEFAULT now()
);


CREATE TABLE IF NOT EXISTS "email_domains"
(
    "id"            UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "domain"        VARCHAR(50)      NOT NULL,
    "university_id" UUID             NOT NULL REFERENCES universities (id) ON DELETE CASCADE
);
