CREATE TABLE cv_document (
    id          UUID PRIMARY KEY,
    ticket      VARCHAR(36)  NOT NULL UNIQUE,
    filename    VARCHAR(255) NOT NULL,
    size_bytes  BIGINT       NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);