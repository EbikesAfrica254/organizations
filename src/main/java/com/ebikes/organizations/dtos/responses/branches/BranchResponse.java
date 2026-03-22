package com.ebikes.organizations.dtos.responses.branches;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.ebikes.organizations.database.models.DaySchedule;
import com.ebikes.organizations.enums.BranchStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BranchResponse(
    BranchAddressResponse address,
    String branchName,
    OffsetDateTime createdAt,
    OffsetDateTime deactivatedAt,
    String deactivationReason,
    String displayName,
    String email,
    UUID id,
    List<DaySchedule> operatingHours,
    UUID organizationId,
    String phoneNumber,
    BranchStatus status,
    OffsetDateTime updatedAt)
    implements Serializable {}
