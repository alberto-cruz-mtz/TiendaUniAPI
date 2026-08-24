CREATE TABLE IF NOT EXISTS "conversations"
(
    "id"             UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    -- sale_person and customer are both users, but they have different roles in the conversation
    "sale_person_id" UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    "customer_id"    UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    "created_at"     TIMESTAMPTZ      NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS "messages"
(
    "id"              UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "content"         TEXT             NOT NULL,
    "created_at"      TIMESTAMPTZ      NOT NULL DEFAULT now(),
    "type_content"    VARCHAR(15)      NOT NULL,
    "conversation_id" UUID             NOT NULL REFERENCES conversations (id) ON DELETE CASCADE,
    "user_id"         UUID             NOT NULL REFERENCES users (id) ON DELETE CASCADE
);