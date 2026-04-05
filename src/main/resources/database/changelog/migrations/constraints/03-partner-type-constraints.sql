--comment: update document_type and registration_type check constraints to reflect new enum values

ALTER TABLE organizations.documents
    DROP CONSTRAINT chk_documents_type;

ALTER TABLE organizations.documents
    ADD CONSTRAINT chk_documents_type
        CHECK (document_type IN (
                                 'BUSINESS_NAME_CERTIFICATE',
                                 'CERTIFICATE_OF_COMPLIANCE',
                                 'CERTIFICATE_OF_INCORPORATION',
                                 'CR12',
                                 'FOREIGN_COMPANY_CERTIFICATE',
                                 'KRA_PIN_CERTIFICATE',
                                 'MEMORANDUM_AND_ARTICLES',
                                 'NATIONAL_ID_BACK',
                                 'NATIONAL_ID_FRONT',
                                 'PARTNERSHIP_DEED',
                                 'PASSPORT',
                                 'PUBLIC_HEALTH_CERTIFICATE',
                                 'SINGLE_BUSINESS_PERMIT'
            ));

ALTER TABLE organizations.organizations
    DROP CONSTRAINT chk_organizations_registration_type;

ALTER TABLE organizations.organizations
    ADD CONSTRAINT chk_organizations_registration_type
        CHECK (registration_type IN (
                                     'FOREIGN_ENTITY',
                                     'INDIVIDUAL',
                                     'PARTNERSHIP',
                                     'PRIVATE_LIMITED_COMPANY',
                                     'SOLE_PROPRIETOR'
            ));