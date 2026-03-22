package com.ebikes.organizations.dtos.responses.documents;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentResponse(
    OffsetDateTime createdAt,
    DocumentType documentType,
    LocalDate expiryDate,
    String fileName,
    Long fileSizeBytes,
    UUID id,
    String mimeType,
    UUID organizationId,
    UUID replacesDocumentId,
    DocumentStatus status,
    OffsetDateTime uploadedAt,
    OffsetDateTime updatedAt) {}
