package com.ebikes.organizations.dtos.requests.organizations;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.enums.BusinessRegistrationType;

public record CreateOrganizationRequest(
    @NotNull(message = "At least one address is required") @Size(min = 1, message = "At least one address is required") @Valid List<Address> addresses,
    @NotNull(message = "At least one document is required") @Size(min = 1, message = "At least one document is required") @Valid List<DocumentUploadInfo> documents,
    @NotBlank(message = "Display name is required") @Size(max = 255, message = "Display name must not exceed 255 characters") String displayName,
    @Email(message = "Email must be valid") @NotBlank(message = "Email is required") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    LocalDate incorporationDate,
    @Pattern(
            regexp = "^[A-Z]\\d{9}[A-Z]$",
            message = "KRA PIN must be one uppercase letter, nine digits, and one uppercase letter")
        @Size(min = 11, max = 11, message = "KRA PIN must be exactly 11 characters") String kraPin,
    @NotBlank(message = "Legal name is required") @Size(max = 255, message = "Legal name must not exceed 255 characters") String legalName,
    @NotBlank(message = "Owner id is required") @Size(min = 36, max = 36, message = "Owner id must be exactly 36 characters") String ownerId,
    @NotBlank(message = "Phone number is required") @Pattern(
            regexp = "^\\+?\\d{10,15}$",
            message = "Phone number must be 10-15 digits with optional + prefix")
        @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber,
    @Size(max = 100, message = "Registration number must not exceed 100 characters") String registrationNumber,
    @NotNull(message = "Registration type is required") BusinessRegistrationType registrationType)
    implements Serializable {

  public CreateOrganizationRequest {
    addresses = addresses != null ? List.copyOf(addresses) : List.of();
    documents = documents != null ? List.copyOf(documents) : List.of();
  }

  @Override
  public List<Address> addresses() {
    return Collections.unmodifiableList(addresses);
  }

  @Override
  public List<DocumentUploadInfo> documents() {
    return Collections.unmodifiableList(documents);
  }
}
