--comment: correct default compliance_status for organizations to NON_COMPLIANT
ALTER TABLE organizations.organizations
    ALTER COLUMN compliance_status SET DEFAULT 'NON_COMPLIANT';