package com.ebikes.organizations.dtos.responses.branches;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.BranchStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BranchSummaryResponse(
    String branchName,
    OffsetDateTime createdAt,
    String displayName,
    String email,
    UUID id,
    UUID organizationId,
    BranchStatus status)
    implements Serializable {}
