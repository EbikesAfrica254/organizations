package com.ebikes.organizations.dtos.requests.branches;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateBranchRequest(@NotBlank @Size(max = 500) String reason) {}
