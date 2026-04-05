package com.ebikes.organizations.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.support.database.FilterUtilities;

public final class OrganizationSpecifications {

  public static final String FIELD_ACTIVATED_AT = "activatedAt";
  public static final String FIELD_COMPLIANCE_STATUS = "complianceStatus";
  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_LEGAL_NAME = "legalName";
  public static final String FIELD_REGISTRATION_TYPE = "registrationType";
  public static final String FIELD_STATUS = "status";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(
          FIELD_ACTIVATED_AT,
          FIELD_COMPLIANCE_STATUS,
          FIELD_CREATED_AT,
          FIELD_LEGAL_NAME,
          FIELD_REGISTRATION_TYPE,
          FIELD_STATUS);

  private OrganizationSpecifications() {
    // prevent instantiation
  }

  public static Specification<Organization> buildSpecification(OrganizationFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          AuthorizationSpecifications.forOrganizations().toPredicate(root, query, criteriaBuilder));

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_ACTIVATED_AT,
          filter.getActivatedAtFrom(),
          filter.getActivatedAtTo());

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getComplianceStatus(),
          hasComplianceStatus(filter.getComplianceStatus()));

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getLegalName(),
          hasLegalName(filter.getLegalName()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getRegistrationType(),
          hasRegistrationType(filter.getRegistrationType()));

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getStatus(),
          hasStatus(filter.getStatus()));

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Organization> hasComplianceStatus(ComplianceStatus complianceStatus) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_COMPLIANCE_STATUS), complianceStatus);
  }

  public static Specification<Organization> hasLegalName(String legalName) {
    return FilterUtilities.likeIgnoreCase(FIELD_LEGAL_NAME, legalName);
  }

  public static Specification<Organization> hasRegistrationType(
      BusinessRegistrationType registrationType) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_REGISTRATION_TYPE), registrationType);
  }

  public static Specification<Organization> hasStatus(OrganizationStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }
}
