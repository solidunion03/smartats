CREATE TABLE job_post (
    id          UUID PRIMARY KEY,
    ticket      VARCHAR(36)  NOT NULL UNIQUE,
    title       VARCHAR(255) NOT NULL,
    raw_text    TEXT         NOT NULL,
    language    VARCHAR(36)  NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);