package com.ebikes.organizations.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.OrganizationRepository;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractRepositoryTest;

@DisplayName("OrganizationSpecifications")
class OrganizationSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private OrganizationRepository repository;

  private Organization active;
  private Organization pendingApproval;
  private Organization deactivated;
  private Organization nonCompliant;
  private Organization soleTrader;

  @BeforeEach
  void setUp() {
    // Default: SYSTEM_ADMIN so all records are visible (no createdBy filter)
    ExecutionContext.set(
        SecurityFixtures.TEST_USER_ID,
        SecurityFixtures.TEST_ORGANIZATION_ID,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        Set.of(UserRole.SYSTEM_ADMIN.name()));

    // Saved references for assertion
    active = OrganizationFixtures.active();
    pendingApproval = OrganizationFixtures.pendingApproval();
    deactivated = OrganizationFixtures.deactivated("test deactivation");

    nonCompliant =
        Organization.builder()
            .complianceStatus(ComplianceStatus.NON_COMPLIANT)
            .displayName("Non-Compliant Org")
            .email("noncompliant@ebikes.test")
            .legalName("Non-Compliant Ltd")
            .ownerId(SecurityFixtures.TEST_USER_ID)
            .phoneNumber(SecurityFixtures.TEST_PHONE_NUMBER)
            .registrationType(BusinessRegistrationType.PRIVATE_LIMITED_COMPANY)
            .status(OrganizationStatus.PENDING_APPROVAL)
            .build();

    soleTrader =
        Organization.builder()
            .complianceStatus(ComplianceStatus.COMPLIANT)
            .displayName("Sole Trader Org")
            .email("soletrader@ebikes.test")
            .legalName("Sole Trader")
            .ownerId(SecurityFixtures.TEST_USER_ID)
            .phoneNumber(SecurityFixtures.TEST_PHONE_NUMBER)
            .registrationType(BusinessRegistrationType.SOLE_PROPRIETOR)
            .status(OrganizationStatus.PENDING_APPROVAL)
            .build();

    repository.saveAll(List.of(active, pendingApproval, deactivated, nonCompliant, soleTrader));
  }

  @AfterEach
  void tearDown() {
    repository.deleteAll();
    ExecutionContext.clear();
  }

  @Nested
  @DisplayName("Filter by status")
  class FilterByStatus {

    @Test
    @DisplayName("should return only ACTIVE organizations")
    void shouldReturnOnlyActiveOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setStatus(OrganizationStatus.ACTIVE);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(active.getId())
          .doesNotContain(
              pendingApproval.getId(),
              deactivated.getId(),
              nonCompliant.getId(),
              soleTrader.getId());
    }

    @Test
    @DisplayName("should return only PENDING_APPROVAL organizations")
    void shouldReturnOnlyPendingApprovalOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setStatus(OrganizationStatus.PENDING_APPROVAL);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(3)
          .allMatch(o -> o.getStatus() == OrganizationStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("should return only DEACTIVATED organizations")
    void shouldReturnOnlyDeactivatedOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setStatus(OrganizationStatus.DEACTIVATED);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(o -> o.getStatus() == OrganizationStatus.DEACTIVATED);
    }
  }

  @Nested
  @DisplayName("Filter by compliance status")
  class FilterByComplianceStatus {

    @Test
    @DisplayName("should return only COMPLIANT organizations")
    void shouldReturnOnlyCompliantOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setComplianceStatus(ComplianceStatus.COMPLIANT);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(soleTrader.getId())
          .doesNotContain(
              active.getId(), pendingApproval.getId(), deactivated.getId(), nonCompliant.getId());
    }

    @Test
    @DisplayName("should return only NON_COMPLIANT organizations")
    void shouldReturnOnlyNonCompliantOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(
              active.getId(), pendingApproval.getId(), deactivated.getId(), nonCompliant.getId())
          .doesNotContain(soleTrader.getId());
    }
  }

  @Nested
  @DisplayName("Filter by legal name")
  class FilterByLegalName {

    @Test
    @DisplayName("should return matching organizations using partial case-insensitive search")
    void shouldReturnMatchingOrganizationsWithPartialCaseInsensitiveSearch() {
      String knownFragment = active.getLegalName().substring(0, 5).toLowerCase();
      OrganizationFilter filter = new OrganizationFilter();

      filter.setLegalName(knownFragment);
      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).extracting(Organization::getId).contains(active.getId());
    }

    @Test
    @DisplayName("should return nothing for an unmatched legal name")
    void shouldReturnNothingForUnmatchedLegalName() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setLegalName("does not exist");

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by registration type")
  class FilterByRegistrationType {

    @Test
    @DisplayName("should return only PRIVATE_LIMITED_COMPANY organizations")
    void shouldReturnOnlyRegisteredCompanyOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setRegistrationType(BusinessRegistrationType.PRIVATE_LIMITED_COMPANY);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(
              active.getId(), pendingApproval.getId(), deactivated.getId(), nonCompliant.getId())
          .doesNotContain(soleTrader.getId());
    }

    @Test
    @DisplayName("should return only SOLE_PROPRIETOR organizations")
    void shouldReturnOnlySoleTraderOrganizations() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setRegistrationType(BusinessRegistrationType.SOLE_PROPRIETOR);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .allMatch(o -> o.getRegistrationType() == BusinessRegistrationType.SOLE_PROPRIETOR);
    }
  }

  @Nested
  @DisplayName("Filter by activated date range")
  class FilterByActivatedAtDateRange {

    @Test
    @DisplayName("activatedAtFrom filters out organizations activated before the threshold")
    void activatedAtFromFiltersOldRecords() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setActivatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("activatedAtTo filters out organizations activated after the threshold")
    void activatedAtToFiltersNewRecords() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setActivatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("activatedAtFrom and activatedAtTo combined returns organizations within range")
    void activatedAtFromAndToReturnsRecordsWithinRange() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setActivatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setActivatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(active.getId(), deactivated.getId())
          .doesNotContain(pendingApproval.getId(), nonCompliant.getId(), soleTrader.getId());
    }
  }

  @Nested
  @DisplayName("Filter by created date range")
  class FilterByCreatedAtDateRange {

    @Test
    @DisplayName("createdAtFrom filters out organizations created before the threshold")
    void createdAtFromFiltersOldRecords() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo filters out organizations created after the threshold")
    void createdAtToFiltersNewRecords() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtFrom and createdAtTo combined returns all organizations within range")
    void createdAtFromAndToReturnsAllRecordsWithinRange() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(
              active.getId(),
              pendingApproval.getId(),
              deactivated.getId(),
              nonCompliant.getId(),
              soleTrader.getId());
    }
  }

  @Nested
  @DisplayName("Authorization filter")
  class AuthorizationFilter {

    @Test
    @DisplayName("should return all organizations for SYSTEM_ADMIN")
    void shouldReturnAllOrganizationsForSystemAdmin() {
      // Context already set to SYSTEM_ADMIN in @BeforeEach
      OrganizationFilter filter = new OrganizationFilter();

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .extracting(Organization::getId)
          .contains(
              active.getId(),
              pendingApproval.getId(),
              deactivated.getId(),
              nonCompliant.getId(),
              soleTrader.getId());
    }

    @Test
    @DisplayName("should return only organizations created by the current user for non-admin")
    void shouldReturnOnlyOwnOrganizationsForRegularUser() {
      String otherUserId = UUID.randomUUID().toString();

      // Save an org owned by a different user
      Organization otherUsersOrg =
          Organization.builder()
              .complianceStatus(ComplianceStatus.COMPLIANT)
              .displayName("Other User Org")
              .email("other@ebikes.test")
              .legalName("Other User Ltd")
              .ownerId(otherUserId)
              .phoneNumber(SecurityFixtures.TEST_PHONE_NUMBER)
              .registrationType(BusinessRegistrationType.PRIVATE_LIMITED_COMPANY)
              .status(OrganizationStatus.PENDING_APPROVAL)
              .build();
      repository.save(otherUsersOrg);

      // Switch to non-admin context — createdBy filter applies
      ExecutionContext.clear();
      ExecutionContext.set(
          SecurityFixtures.TEST_USER_ID,
          SecurityFixtures.TEST_ORGANIZATION_ID,
          null,
          SecurityFixtures.TEST_EMAIL,
          Set.of(),
          SecurityFixtures.TEST_PHONE_NUMBER,
          Set.of(UserRole.ORGANIZATION_ADMIN.name()));

      OrganizationFilter filter = new OrganizationFilter();

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .isNotEmpty()
          .allMatch(o -> o.getCreatedBy().equals(SecurityFixtures.TEST_USER_ID));
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("status and complianceStatus combined returns correct subset")
    void statusAndComplianceStatusCombined() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setStatus(OrganizationStatus.PENDING_APPROVAL);
      filter.setComplianceStatus(ComplianceStatus.NON_COMPLIANT);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .allMatch(
              o ->
                  o.getStatus() == OrganizationStatus.PENDING_APPROVAL
                      && o.getComplianceStatus() == ComplianceStatus.NON_COMPLIANT);
    }

    @Test
    @DisplayName("status and registrationType combined returns correct subset")
    void statusAndRegistrationTypeCombined() {
      OrganizationFilter filter = new OrganizationFilter();
      filter.setStatus(OrganizationStatus.PENDING_APPROVAL);
      filter.setRegistrationType(BusinessRegistrationType.SOLE_PROPRIETOR);

      List<Organization> results =
          repository.findAll(OrganizationSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .first()
          .satisfies(
              o -> {
                assertThat(o.getStatus()).isEqualTo(OrganizationStatus.PENDING_APPROVAL);
                assertThat(o.getRegistrationType())
                    .isEqualTo(BusinessRegistrationType.SOLE_PROPRIETOR);
              });
    }
  }
}
