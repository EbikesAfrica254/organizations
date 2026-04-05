package com.ebikes.organizations.support.fixtures;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.BranchStatus;

public final class BranchFixtures {

  private BranchFixtures() {}

  public static Branch active(Organization organization) {
    return base(organization).build();
  }

  public static Branch deactivated(Organization organization, String reason) {
    Branch branch = active(organization);
    branch.suspend();
    branch.deactivate(reason);
    return branch;
  }

  public static Branch suspended(Organization organization) {
    Branch branch = active(organization);
    branch.suspend();
    return branch;
  }

  private static Branch.BranchBuilder<?, ?> base(Organization organization) {
    return Branch.builder()
        .branchName("Test Branch")
        .displayName("Test Branch Display")
        .email("branch@ebikes.test")
        .organization(organization)
        .phoneNumber(SecurityFixtures.TEST_PHONE_NUMBER)
        .status(BranchStatus.ACTIVE);
  }
}
