--comment: add ARCHIVED to the document status check constraint to support stale upload archival
ALTER TABLE organizations.documents
    DROP CONSTRAINT chk_documents_status;

ALTER TABLE organizations.documents
    ADD CONSTRAINT chk_documents_status
        CHECK (status IN ('ACTIVE', 'ARCHIVED', 'EXPIRED', 'PENDING', 'REPLACED', 'UPLOADED'));

--comment: update status column comment to reflect ARCHIVED transition
COMMENT ON COLUMN organizations.documents.status IS
    'Document lifecycle status. Valid transitions: PENDING→UPLOADED, UPLOADED→ACTIVE, '
        'ACTIVE→REPLACED, ACTIVE→EXPIRED, EXPIRED→REPLACED, UPLOADED→ARCHIVED (stale upload cleanup). '
        'ARCHIVED is a terminal state for uploads that were never confirmed or approved.';