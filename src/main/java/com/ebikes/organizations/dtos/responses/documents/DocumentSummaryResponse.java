package com.ebikes.organizations.dtos.responses.documents;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentSummaryResponse(
    DocumentType documentType,
    LocalDate expiryDate,
    String fileName,
    UUID id,
    UUID organizationId,
    DocumentStatus status,
    OffsetDateTime uploadedAt) {}
