--comment: add expired_at column to documents to record when a document was expired by the expiry job
ALTER TABLE organizations.documents
    ADD COLUMN expired_at TIMESTAMPTZ;

--comment: add check constraint ensuring expired_at is only set when status is EXPIRED
ALTER TABLE organizations.documents
    ADD CONSTRAINT chk_documents_expired_at_requires_expired
        CHECK (expired_at IS NULL OR status = 'EXPIRED');

--comment: add index to support expiry job queries on expired_at
CREATE INDEX idx_documents_expired_at ON organizations.documents (expired_at)
    WHERE expired_at IS NOT NULL;

--comment: add column comment for expired_at
COMMENT ON COLUMN organizations.documents.expired_at IS
    'Timestamp when the document was transitioned to EXPIRED by the expiry job - null until expiry occurs. '
        'Distinct from expiry_date (the scheduled date) — the gap between them indicates job execution lag.';