package com.ebikes.organizations.database.specifications;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.AuthorizationException;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.security.RBACUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AuthorizationSpecifications {

  private static final UUID BASE_ORGANIZATION_ID =
      UUID.fromString(ApplicationConstants.BASE_ORGANIZATION_ID);

  private AuthorizationSpecifications() {
    throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
  }

  public static void assertBranchOwnership(Branch branch, UUID organizationId) {
    if (!branch.getOrganization().getId().equals(organizationId)) {
      log.error(
          "Branch ownership mismatch: branchId={}, expectedOrgId={}, actualOrgId={}",
          branch.getId(),
          organizationId,
          branch.getOrganization().getId());
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Branch does not belong to the specified organization");
    }
    assertOrganizationAccess(organizationId);
  }

  public static void assertOrganizationAccess(UUID organizationId) {
    Set<String> roles = ExecutionContext.getRoles();
    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(roles))) {
      return;
    }
    String activeOrganization = validateActiveOrganization();
    if (!organizationId.toString().equals(activeOrganization)) {
      log.error(
          "Organization access denied: requestedOrgId={}, activeOrgId={}",
          organizationId,
          activeOrganization);
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Access denied to the specified organization");
    }
  }

  public static Specification<Organization> forOrganizations() {
    Set<String> roles = ExecutionContext.getRoles();
    Specification<Organization> excludeBaseOrg = excludeBaseOrganization();

    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(roles))) {
      log.debug("SYSTEM_ADMIN access: returning all organizations excluding base");
      return excludeBaseOrg;
    }

    String userId = ExecutionContext.getUserId();
    log.debug("Filtering organizations by createdBy: userId={}", userId);

    return excludeBaseOrg.and(filterByCreatedBy(userId));
  }

  private static Specification<Organization> excludeBaseOrganization() {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.notEqual(root.get("id"), BASE_ORGANIZATION_ID);
  }

  private static Specification<Organization> filterByCreatedBy(String userId) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("createdBy"), userId);
  }

  private static String validateActiveOrganization() {
    String activeOrganization = ExecutionContext.getActiveOrganization();
    if (activeOrganization == null || activeOrganization.isBlank()) {
      log.error("Missing active_organization claim for {} access", "organization");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return activeOrganization;
  }
}
