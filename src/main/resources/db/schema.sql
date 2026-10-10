CREATE TABLE IF NOT EXISTS purchase_request (
    id            uuid PRIMARY KEY,
    business_key  text NOT NULL UNIQUE,
    status        text NOT NULL,
    title         text NOT NULL,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT purchase_request_status_known
        CHECK (status IN ('DRAFT', 'APPROVED', 'ORDERED'))
);
