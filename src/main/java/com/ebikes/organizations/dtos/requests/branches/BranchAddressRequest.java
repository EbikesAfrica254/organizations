package com.ebikes.organizations.dtos.requests.branches;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BranchAddressRequest(
    @NotBlank(message = "City is required") @Size(max = 100, message = "City must not exceed 100 characters") String city,
    @NotBlank(message = "Country is required") @Size(max = 100, message = "Country must not exceed 100 characters") String country,
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90") @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90") BigDecimal latitude,
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180") @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180") BigDecimal longitude,
    @Size(max = 20, message = "Postal code must not exceed 20 characters") String postalCode,
    @NotBlank(message = "Street address is required") @Size(max = 500, message = "Street address must not exceed 500 characters") String streetAddress) {}
