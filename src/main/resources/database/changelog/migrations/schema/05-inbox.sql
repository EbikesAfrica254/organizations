--comment: create an inbox table for idempotent event consumption with deduplication
CREATE TABLE organizations.inbox (
     service_reference  VARCHAR(255)  NOT NULL,
     event_type         VARCHAR(100)  NOT NULL,
     processed_at       TIMESTAMPTZ,
     received_at        TIMESTAMPTZ   NOT NULL,
     source_context     VARCHAR(100)  NOT NULL,

     CONSTRAINT pk_inbox
         PRIMARY KEY (service_reference)
);

--comment: add check constraints for the inbox table
ALTER TABLE organizations.inbox
    ADD CONSTRAINT chk_inbox_processed_after_received
        CHECK (processed_at IS NULL OR processed_at >= received_at);

--comment: create indexes for the inbox table
CREATE INDEX idx_inbox_processed_at   ON organizations.inbox (processed_at);
CREATE INDEX idx_inbox_source_context ON organizations.inbox (source_context);

--comment: add table and column comments for inbox
COMMENT ON TABLE organizations.inbox IS
    'Inbox pattern for idempotent event consumption. Deduplicates at-least-once delivered events '
        'from upstream bounded contexts using service_reference as the natural deduplication key. '
        'processed_at is null until processing completes successfully.';

COMMENT ON COLUMN organizations.inbox.service_reference IS 'Primary key - unique event identifier from publisher, prevents duplicate processing';
COMMENT ON COLUMN organizations.inbox.event_type        IS 'Event type name from the upstream publishing context';
COMMENT ON COLUMN organizations.inbox.processed_at      IS 'Timestamp when processing completed - null until InboxService.markProcessed() is called';
COMMENT ON COLUMN organizations.inbox.received_at       IS 'Timestamp when event first arrived - set by application on persist';
COMMENT ON COLUMN organizations.inbox.source_context    IS 'Originating bounded context identifier';