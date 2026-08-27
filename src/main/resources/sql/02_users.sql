CREATE TABLE IF NOT EXISTS "users"
(
    "id"            UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "email"         VARCHAR(150)     NOT NULL UNIQUE,
    "password"      VARCHAR(200)     NOT NULL,
    "first_name"    VARCHAR(60)      NOT NULL,
    "last_name"     VARCHAR(60)      NOT NULL,
        "avatar_url"    VARCHAR(300)     NOT NULL,
    "verified"      BOOLEAN          NOT NULL DEFAULT FALSE,
    "created_at"    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "updated_at"    TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "university_id" UUID             NOT NULL REFERENCES universities (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS "roles"
(
    "id"   SERIAL PRIMARY KEY NOT NULL,
    "name" VARCHAR(40)        NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS "permissions"
(
    "id"   SERIAL PRIMARY KEY NOT NULL,
    "name" VARCHAR(40)        NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS "rol_permission"
(
    "rol_id"        INTEGER NOT NULL,
    "permission_id" INTEGER NOT NULL,
    PRIMARY KEY ("rol_id", "permission_id")
);

CREATE TABLE IF NOT EXISTS "user_role"
(
    "user_id" UUID    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    "rol_id"  INTEGER NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    PRIMARY KEY ("user_id", "rol_id")
);

-- Migration: add verified column to existing users table (idempotent).
ALTER TABLE "users"
    ADD COLUMN IF NOT EXISTS "verified" BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE IF NOT EXISTS "refresh_tokens"
(
    "id"         UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "token"      VARCHAR(32)      NOT NULL,
    "created_at" TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "updated_at" TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "expired_at" TIMESTAMPTZ,
    "revoked"    BOOLEAN                   DEFAULT FALSE,
    "user_id"    UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE
);
