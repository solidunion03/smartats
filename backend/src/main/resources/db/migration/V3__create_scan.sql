CREATE TABLE scan (
    id           UUID PRIMARY KEY,
    ticket       VARCHAR(36) NOT NULL UNIQUE,
    cv_id        UUID NOT NULL,
    job_post_id  UUID NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);