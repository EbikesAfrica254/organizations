package com.ebikes.organizations.database.specifications;

import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.AuthorizationException;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.security.RBACUtilities;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class AuthorizationSpecifications {

  private AuthorizationSpecifications() {
    // prevent instantiation
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
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Organization access is not permitted in system context");
    }

    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(ctx.roles()))) {
      return;
    }

    String activeOrganization = validateActiveOrganization(ctx);
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
    if (!(ExecutionContext.get() instanceof ExecutionContext.UserContext ctx)) {
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Organizations are not accessible in system context");
    }

    if (RBACUtilities.hasSystemAdminRole(RBACUtilities.parseRoles(ctx.roles()))) {
      return noFilter();
    }

    return filterByCreatedBy(ctx.userId());
  }

  private static <T> Specification<T> noFilter() {
    return (root, query, cb) -> cb.conjunction();
  }

  private static Specification<Organization> filterByCreatedBy(String userId) {
    return (root, query, cb) -> cb.equal(root.get("createdBy"), userId);
  }

  private static String validateActiveOrganization(ExecutionContext.UserContext ctx) {
    String activeOrganization = ctx.activeOrganization();
    if (activeOrganization == null || activeOrganization.isBlank()) {
      log.error("Missing active_organization claim for organization access");
      throw new AuthorizationException(
          ResponseCode.FORBIDDEN, "Active organization context required for this operation");
    }
    return activeOrganization;
  }
}
