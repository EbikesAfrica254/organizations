package com.ebikes.organizations.dtos.requests.documents;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DocumentUploadConfirmationRequest(
    @NotNull(message = "File size is required") @Min(value = 1, message = "File size must be at least 1 byte") Long fileSizeBytes,
    @NotBlank(message = "MIME type is required") String mimeType) {}
