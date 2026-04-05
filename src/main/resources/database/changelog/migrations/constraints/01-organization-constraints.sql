--comment: fix chk_organizations_activation_requires_active to permit activated_at on DEACTIVATED organizations
ALTER TABLE organizations.organizations
    DROP CONSTRAINT chk_organizations_activation_requires_active;

ALTER TABLE organizations.organizations
    ADD CONSTRAINT chk_organizations_activation_requires_active
        CHECK (activated_at IS NULL OR status IN ('ACTIVE', 'DEACTIVATED'));