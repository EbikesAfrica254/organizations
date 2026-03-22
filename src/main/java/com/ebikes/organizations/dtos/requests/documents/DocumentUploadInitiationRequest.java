package com.ebikes.organizations.dtos.requests.documents;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.enums.DocumentType;

public record DocumentUploadInitiationRequest(
    @NotBlank(message = "Content type is required") String contentType,
    @NotNull(message = "Document type is required") DocumentType documentType,
    @NotBlank(message = "File name is required") @Size(max = 255, message = "File name must not exceed 255 characters") String fileName) {}
