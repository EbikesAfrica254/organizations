package com.ebikes.organizations.dtos.requests.organizations;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ImageUploadConfirmationRequest(
    @NotNull @Min(value = 1) Long fileSizeBytes, @NotBlank @Size(max = 100) String mimeType) {}
