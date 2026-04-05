package com.ebikes.organizations.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.exceptions.AuthorizationException;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.fixtures.BranchFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;

@DisplayName("AuthorizationSpecifications")
class AuthorizationSpecificationsTest {

  private static final UUID TEST_ORG_ID = UUID.fromString(SecurityFixtures.TEST_ORGANIZATION_ID);

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
  }

  private void setUserContext(UUID activeOrgId, UserRole... roles) {
    Set<String> roleNames =
        roles.length == 0
            ? Set.of()
            : Set.of(java.util.Arrays.stream(roles).map(UserRole::name).toArray(String[]::new));
    ExecutionContext.set(
        SecurityFixtures.TEST_USER_ID,
        activeOrgId != null ? activeOrgId.toString() : null,
        null,
        SecurityFixtures.TEST_EMAIL,
        Set.of(),
        SecurityFixtures.TEST_PHONE_NUMBER,
        roleNames);
  }

  @Nested
  @DisplayName("assertOrganizationAccess")
  class AssertOrganizationAccess {

    @Test
    @DisplayName("should throw FORBIDDEN when context is SystemContext")
    void shouldThrowWhenSystemContext() {
      ExecutionContext.setSystem();

      assertThatThrownBy(() -> AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should pass for SYSTEM_ADMIN regardless of active organization")
    void shouldPassForSystemAdmin() {
      UUID differentOrgId = UUID.randomUUID();
      setUserContext(differentOrgId, UserRole.SYSTEM_ADMIN);

      // SYSTEM_ADMIN bypasses org check — should not throw
      AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID);
    }

    @Test
    @DisplayName("should pass when active organization matches requested organization")
    void shouldPassWhenOrganizationMatches() {
      setUserContext(TEST_ORG_ID, UserRole.ORGANIZATION_ADMIN);

      AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID);
    }

    @Test
    @DisplayName("should throw FORBIDDEN when active organization does not match")
    void shouldThrowWhenOrganizationMismatch() {
      setUserContext(UUID.randomUUID(), UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(() -> AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should throw FORBIDDEN when active organization is null")
    void shouldThrowWhenActiveOrganizationIsNull() {
      setUserContext(null, UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(() -> AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should throw FORBIDDEN when active organization is blank")
    void shouldThrowWhenActiveOrganizationIsBlank() {
      ExecutionContext.set(
          SecurityFixtures.TEST_USER_ID,
          "   ",
          null,
          SecurityFixtures.TEST_EMAIL,
          Set.of(),
          SecurityFixtures.TEST_PHONE_NUMBER,
          Set.of(UserRole.ORGANIZATION_ADMIN.name()));

      assertThatThrownBy(() -> AuthorizationSpecifications.assertOrganizationAccess(TEST_ORG_ID))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("assertBranchOwnership")
  class AssertBranchOwnership {

    private Branch branch;

    @BeforeEach
    void setUp() {
      Organization organization = OrganizationFixtures.active();
      ReflectionTestUtils.setField(organization, "id", TEST_ORG_ID);
      branch = BranchFixtures.active(organization);
    }

    @Test
    @DisplayName("should pass when branch belongs to organization and user has org access")
    void shouldPassWhenBranchBelongsToOrganizationAndUserHasAccess() {
      setUserContext(TEST_ORG_ID, UserRole.ORGANIZATION_ADMIN);

      AuthorizationSpecifications.assertBranchOwnership(branch, TEST_ORG_ID);
    }

    @Test
    @DisplayName("should throw FORBIDDEN when branch belongs to a different organization")
    void shouldThrowWhenBranchBelongsToDifferentOrganization() {
      setUserContext(TEST_ORG_ID, UserRole.ORGANIZATION_ADMIN);

      UUID differentOrgId = UUID.randomUUID();

      assertThatThrownBy(
              () -> AuthorizationSpecifications.assertBranchOwnership(branch, differentOrgId))
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should throw FORBIDDEN when branch org matches but user active org differs")
    void shouldThrowWhenBranchOrgMatchesButUserOrgDiffers() {
      setUserContext(UUID.randomUUID(), UserRole.ORGANIZATION_ADMIN);

      assertThatThrownBy(
              () -> AuthorizationSpecifications.assertBranchOwnership(branch, TEST_ORG_ID))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("forOrganizations")
  class ForOrganizations {

    @Test
    @DisplayName("should throw FORBIDDEN when context is SystemContext")
    void shouldThrowWhenSystemContext() {
      ExecutionContext.setSystem();

      assertThatThrownBy(AuthorizationSpecifications::forOrganizations)
          .isInstanceOf(AuthorizationException.class);
    }

    @Test
    @DisplayName("should return no-op conjunction for SYSTEM_ADMIN")
    void shouldReturnNoFilterForSystemAdmin() {
      setUserContext(TEST_ORG_ID, UserRole.SYSTEM_ADMIN);

      Root<Organization> root = mock(Root.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      Predicate conjunction = mock(Predicate.class);
      when(cb.conjunction()).thenReturn(conjunction);

      Predicate result =
          AuthorizationSpecifications.forOrganizations().toPredicate(root, query, cb);

      assertThat(result).isSameAs(conjunction);
    }

    @Test
    @DisplayName("should return createdBy filter for non-admin user")
    void shouldReturnCreatedByFilterForRegularUser() {
      setUserContext(TEST_ORG_ID, UserRole.ORGANIZATION_ADMIN);

      Root<Organization> root = mock(Root.class);
      CriteriaQuery<?> query = mock(CriteriaQuery.class);
      CriteriaBuilder cb = mock(CriteriaBuilder.class);
      Path<Object> createdByPath = mock(Path.class);
      Predicate equalPredicate = mock(Predicate.class);

      when(root.get("createdBy")).thenReturn(createdByPath);
      when(cb.equal(createdByPath, SecurityFixtures.TEST_USER_ID)).thenReturn(equalPredicate);

      Predicate result =
          AuthorizationSpecifications.forOrganizations().toPredicate(root, query, cb);

      assertThat(result).isSameAs(equalPredicate);
    }
  }
}
