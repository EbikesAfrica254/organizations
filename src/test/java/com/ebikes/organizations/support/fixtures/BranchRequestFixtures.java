package com.ebikes.organizations.support.fixtures;

import java.math.BigDecimal;

import com.ebikes.organizations.dtos.requests.branches.BranchAddressRequest;
import com.ebikes.organizations.dtos.requests.branches.CreateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.DeactivateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.UpdateBranchRequest;

public final class BranchRequestFixtures {

  private BranchRequestFixtures() {}

  public static CreateBranchRequest create() {
    return new CreateBranchRequest(
        defaultAddress(),
        "East Branch",
        "East Branch Display",
        "east@ebikes.test",
        null,
        SecurityFixtures.TEST_PHONE_NUMBER);
  }

  public static UpdateBranchRequest update() {
    return new UpdateBranchRequest(null, "Updated Branch", null, null, null, null);
  }

  public static DeactivateBranchRequest deactivate() {
    return new DeactivateBranchRequest("Compliance issue");
  }

  private static BranchAddressRequest defaultAddress() {
    return new BranchAddressRequest(
        "Nairobi",
        "Kenya",
        BigDecimal.valueOf(-1.286389),
        BigDecimal.valueOf(36.817223),
        "00100",
        "123 Main Street");
  }
}
