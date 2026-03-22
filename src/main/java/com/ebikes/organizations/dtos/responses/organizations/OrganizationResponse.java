package com.ebikes.organizations.dtos.responses.organizations;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrganizationResponse(
    OffsetDateTime activatedAt,
    List<AddressResponse> addresses,
    OffsetDateTime approvedAt,
    ComplianceStatus complianceStatus,
    OffsetDateTime createdAt,
    String createdBy,
    OffsetDateTime deactivatedAt,
    String displayName,
    String email,
    UUID id,
    LocalDate incorporationDate,
    String kraPin,
    String legalName,
    String phoneNumber,
    String registrationNumber,
    BusinessRegistrationType registrationType,
    String rejectionReason,
    OrganizationStatus status,
    OffsetDateTime updatedAt,
    String updatedBy) {}
