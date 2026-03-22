package com.ebikes.organizations.dtos.requests.branches;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.ebikes.organizations.database.models.DaySchedule;

public record CreateBranchRequest(
    @NotNull(message = "Branch address is required") @Valid BranchAddressRequest address,
    @NotBlank(message = "Branch name is required") @Size(max = 255, message = "Branch name must not exceed 255 characters") String branchName,
    @NotBlank(message = "Display name is required") @Size(max = 255, message = "Display name must not exceed 255 characters") String displayName,
    @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
    @Valid List<DaySchedule> operatingHours,
    @NotBlank(message = "Phone number is required") @Pattern(
            regexp = "^\\+?\\d{10,15}$",
            message = "Phone number must be 10-15 digits with optional + prefix")
        @Size(max = 20, message = "Phone number must not exceed 20 characters") String phoneNumber)
    implements Serializable {

  public CreateBranchRequest {
    operatingHours = operatingHours != null ? List.copyOf(operatingHours) : List.of();
  }

  @Override
  public List<DaySchedule> operatingHours() {
    return Collections.unmodifiableList(operatingHours);
  }
}
