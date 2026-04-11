package com.ebikes.organizations.dtos.requests.documents;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.enums.DocumentType;

public record DocumentUploadInitiationRequest(
    @NotBlank String templateContentType,
    @NotNull DocumentType documentType,
    @Future LocalDate expiryDate,
    @NotBlank @Size(max = 255, message = "File name must not exceed 255 characters") String fileName) {}
