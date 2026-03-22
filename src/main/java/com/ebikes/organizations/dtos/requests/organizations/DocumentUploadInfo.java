package com.ebikes.organizations.dtos.requests.organizations;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.enums.DocumentType;

public record DocumentUploadInfo(
    @NotNull(message = "Document type is required") DocumentType documentType,
    @Future(message = "Expiry date must be in the future") LocalDate expiryDate,
    @NotBlank(message = "File name is required") @Size(max = 255, message = "File name must not exceed 255 characters") String fileName,
    @NotNull(message = "File size is required") @Min(value = 1, message = "File size must be at least 1 byte") Long fileSizeBytes,
    @NotBlank(message = "MIME type is required") @Size(max = 100, message = "MIME type must not exceed 100 characters") String mimeType,
    @NotBlank(message = "Storage key is required") @Size(max = 1000, message = "Storage key must not exceed 1000 characters") String key) {}
