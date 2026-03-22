package com.ebikes.organizations.database.specifications;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.support.database.FilterUtilities;

public final class OutboxSpecifications {

  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EVENT_TYPE = "eventType";
  public static final String FIELD_RETRY_COUNT = "retryCount";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CREATED_AT, FIELD_EVENT_TYPE, FIELD_RETRY_COUNT, FIELD_STATUS, FIELD_UPDATED_AT);

  private OutboxSpecifications() {
    // prevent instantiation
  }

  public static Specification<Outbox> buildSpecification(OutboxFilter filter) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();

      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getEventType(),
          hasEventType(filter.getEventType()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getStatus(),
          hasStatus(filter.getStatus()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getMinRetryCount(),
          hasMinRetryCount(filter.getMinRetryCount()));
      FilterUtilities.addIfPresent(
          predicates,
          root,
          query,
          criteriaBuilder,
          filter.getMaxRetryCount(),
          hasMaxRetryCount(filter.getMaxRetryCount()));

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtAfter(),
          filter.getCreatedAtBefore());
      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_UPDATED_AT,
          filter.getUpdatedAtAfter(),
          filter.getUpdatedAtBefore());

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Outbox> hasEventType(String eventType) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.equal(root.get(FIELD_EVENT_TYPE), eventType);
  }

  public static Specification<Outbox> hasMaxRetryCount(Integer maxRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.lessThanOrEqualTo(root.get(FIELD_RETRY_COUNT), maxRetryCount);
  }

  public static Specification<Outbox> hasMinRetryCount(Integer minRetryCount) {
    return (root, query, criteriaBuilder) ->
        criteriaBuilder.greaterThanOrEqualTo(root.get(FIELD_RETRY_COUNT), minRetryCount);
  }

  public static Specification<Outbox> hasStatus(OutboxStatus status) {
    return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get(FIELD_STATUS), status);
  }
}
