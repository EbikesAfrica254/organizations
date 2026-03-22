package com.ebikes.organizations.dtos.responses.branches;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.AddressTag;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BranchAddressResponse(
    AddressTag addressTag,
    String city,
    String country,
    OffsetDateTime createdAt,
    UUID id,
    BigDecimal latitude,
    BigDecimal longitude,
    String postalCode,
    String streetAddress,
    OffsetDateTime updatedAt) {}
