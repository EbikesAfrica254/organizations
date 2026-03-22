package com.ebikes.organizations.dtos.requests.branches;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.database.models.DaySchedule;

public record UpdateBranchRequest(
    @Valid BranchAddressRequest address,
    @Size(max = 255, message = "Branch name must not exceed 255 characters") String branchName,
    @Size(max = 255, message = "Display name must not exceed 255 characters") String displayName,
    @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    @Valid List<DaySchedule> operatingHours,
    @Pattern(
            regexp = "^\\+?\\d{10,15}$",
            message = "Phone number must be 10-15 digits with optional + prefix")
        @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber)
    implements Serializable {

  public UpdateBranchRequest {
    operatingHours = operatingHours != null ? List.copyOf(operatingHours) : null;
  }

  @Override
  public List<DaySchedule> operatingHours() {
    return operatingHours != null ? Collections.unmodifiableList(operatingHours) : null;
  }
}
