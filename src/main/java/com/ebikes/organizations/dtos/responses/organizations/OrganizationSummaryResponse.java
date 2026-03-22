package com.ebikes.organizations.dtos.responses.organizations;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganizationSummaryResponse(
    OffsetDateTime activatedAt,
    ComplianceStatus complianceStatus,
    OffsetDateTime createdAt,
    String displayName,
    String email,
    UUID id,
    String legalName,
    BusinessRegistrationType registrationType,
    OrganizationStatus status) {}
