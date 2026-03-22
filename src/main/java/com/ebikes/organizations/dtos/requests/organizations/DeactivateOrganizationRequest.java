package com.ebikes.organizations.dtos.requests.organizations;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateOrganizationRequest(@NotBlank @Size(max = 500) String reason) {}
