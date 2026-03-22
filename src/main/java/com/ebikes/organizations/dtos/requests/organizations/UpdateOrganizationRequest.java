package com.ebikes.organizations.dtos.requests.organizations;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.enums.BusinessRegistrationType;

public record UpdateOrganizationRequest(
    @Valid List<Address> addresses,
    @Valid List<DocumentUploadInfo> documents,
    @Size(max = 255, message = "Display name must not exceed 255 characters") String displayName,
    @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    LocalDate incorporationDate,
    @Pattern(
            regexp = "^[A-Z]\\d{9}[A-Z]$",
            message = "KRA PIN must be one uppercase letter, nine digits, and one uppercase letter")
        @Size(min = 11, max = 11, message = "KRA PIN must be exactly 11 characters") String kraPin,
    @Size(max = 255, message = "Legal name must not exceed 255 characters") String legalName,
    @Pattern(
            regexp = "^\\+?\\d{10,15}$",
            message = "Phone number must be 10-15 digits with optional + prefix")
        @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber,
    @Size(max = 100, message = "Registration number must not exceed 100 characters") String registrationNumber,
    BusinessRegistrationType registrationType)
    implements Serializable {

  public UpdateOrganizationRequest {
    addresses = addresses != null ? List.copyOf(addresses) : null;
    documents = documents != null ? List.copyOf(documents) : null;
  }

  @Override
  public List<Address> addresses() {
    return addresses != null ? Collections.unmodifiableList(addresses) : null;
  }

  @Override
  public List<DocumentUploadInfo> documents() {
    return documents != null ? Collections.unmodifiableList(documents) : null;
  }
}
