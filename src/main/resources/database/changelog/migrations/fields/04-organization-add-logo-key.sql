--comment: add logo_key column to organizations table
ALTER TABLE organizations.organizations
    ADD COLUMN logo_key VARCHAR(500);

COMMENT ON COLUMN organizations.organizations.logo_key IS 'S3 object key for the organization logo image - null until a logo is uploaded. Stored under logos/<orgId>/logo prefix in S3.';
