package com.ebikes.organizations.support.fixtures;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;

import net.datafaker.Faker;

public final class OrganizationFixtures {

  private static final Faker FAKER = new Faker();

  private OrganizationFixtures() {}

  public static Organization active() {
    Organization org = pendingApproval();
    org.approve();
    org.activate();
    return org;
  }

  public static Organization deactivated(String reason) {
    Organization org = active();
    org.deactivate(reason);
    return org;
  }

  public static Organization pendingApproval() {
    return base().build();
  }

  public static Organization rejected(String reason) {
    Organization org = pendingApproval();
    org.reject(reason);
    return org;
  }

  private static Organization.OrganizationBuilder<?, ?> base() {
    String company = FAKER.company().name();
    return Organization.builder()
        .complianceStatus(ComplianceStatus.NON_COMPLIANT)
        .displayName(company)
        .email(FAKER.internet().emailAddress())
        .legalName(company + " " + FAKER.numerify("####") + " Ltd")
        .ownerId(SecurityFixtures.TEST_USER_ID)
        .phoneNumber(SecurityFixtures.TEST_PHONE_NUMBER)
        .registrationType(BusinessRegistrationType.PRIVATE_LIMITED_COMPANY)
        .status(OrganizationStatus.PENDING_APPROVAL);
  }
}
