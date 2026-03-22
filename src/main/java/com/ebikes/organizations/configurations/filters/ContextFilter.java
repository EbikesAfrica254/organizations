package com.ebikes.organizations.configurations.filters;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.constants.MDCKeys;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.references.ReferenceGenerator;
import com.ebikes.organizations.support.web.IpAddressUtilities;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ContextFilter extends OncePerRequestFilter {

  private static final String ACTIVE_BRANCH_CLAIM = "active_branch";
  private static final String ACTIVE_ORGANIZATION_CLAIM = "active_organization";
  private static final String GROUPS_CLAIM = "groups";
  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_KEY = "roles";
  private static final String SUB_CLAIM = "sub";

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      if (authentication instanceof JwtAuthenticationToken jwtAuth) {
        Jwt token = jwtAuth.getToken();
        String userId = token.getClaimAsString(SUB_CLAIM);

        if (userId != null && !userId.isBlank()) {
          String activeOrganization = token.getClaimAsString(ACTIVE_ORGANIZATION_CLAIM);
          String activeBranch = token.getClaimAsString(ACTIVE_BRANCH_CLAIM);
          String ipAddress = IpAddressUtilities.getClientIpAddress(request);
          Set<String> roles = extractRoles(token);
          Set<String> groups = extractGroups(token);

          ExecutionContext.set(userId, activeOrganization, activeBranch, groups, roles);

          populateMDC(request, userId, ipAddress, activeOrganization, activeBranch);

          log.debug(
              "Execution context set: userId={}, ipAddress={}, activeOrganization={},"
                  + " activeBranch={}, roles={}, groups={}, path={}",
              userId,
              ipAddress,
              activeOrganization,
              activeBranch,
              roles,
              groups,
              request.getRequestURI());
        } else {
          log.warn(
              "JWT missing sub claim: path={}, subject={}",
              request.getRequestURI(),
              token.getSubject());
        }
      }

      filterChain.doFilter(request, response);

    } finally {
      ExecutionContext.clear();
      MDC.clear();
    }
  }

  private Set<String> extractGroups(Jwt token) {
    List<String> groupsList = token.getClaimAsStringList(GROUPS_CLAIM);
    return groupsList != null ? new HashSet<>(groupsList) : Collections.emptySet();
  }

  @SuppressWarnings("unchecked")
  private Set<String> extractRoles(Jwt token) {
    try {
      Map<String, Object> realmAccess = token.getClaim(REALM_ACCESS_CLAIM);
      if (realmAccess != null && realmAccess.containsKey(ROLES_KEY)) {
        Object rolesObj = realmAccess.get(ROLES_KEY);
        if (rolesObj instanceof List<?> rolesList) {
          return new HashSet<>((List<String>) rolesList);
        }
      }
    } catch (Exception e) {
      log.warn("Failed to extract roles from JWT: {}", e.getMessage());
    }
    return Collections.emptySet();
  }

  private void populateMDC(
      HttpServletRequest request,
      String userId,
      String ipAddress,
      String activeOrganization,
      String activeBranch) {

    MDC.put(MDCKeys.USER_ID, userId);
    MDC.put(MDCKeys.IP_ADDRESS, ipAddress);
    MDC.put(MDCKeys.REQUEST_PATH, request.getRequestURI());

    if (activeOrganization != null) {
      MDC.put(MDCKeys.ACTIVE_ORGANIZATION, activeOrganization);
    }

    if (activeBranch != null) {
      MDC.put(MDCKeys.ACTIVE_BRANCH, activeBranch);
    }

    String requestId = request.getHeader(ApplicationConstants.REQUEST_ID_HEADER);
    if (requestId == null || requestId.isBlank()) {
      requestId = ReferenceGenerator.generateErrorReference();
    }
    MDC.put(MDCKeys.REQUEST_ID, requestId);
  }
}
