-- update outbox status enum
ALTER TABLE organizations.outbox DROP CONSTRAINT chk_outbox_status;
ALTER TABLE organizations.outbox
    ADD CONSTRAINT chk_outbox_status
        CHECK (status IN ('DEAD_LETTER', 'FAILED', 'PENDING', 'SENT'));
