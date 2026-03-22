package com.ebikes.organizations.database.models;

import java.io.Serializable;
import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.enums.AddressTag;

public record Address(
    @NotNull AddressTag addressTag,
    @NotBlank(message = "City is required") @Size(max = 100) String city,
    @NotBlank(message = "Country is required") @Size(max = 100) String country,
    @DecimalMin(value = "-90.0") @DecimalMax(value = "90.0") BigDecimal latitude,
    @DecimalMin(value = "-180.0") @DecimalMax(value = "180.0") BigDecimal longitude,
    @Size(max = 20) String postalCode,
    @NotBlank @Size(max = 500) String streetAddress)
    implements Serializable {}
