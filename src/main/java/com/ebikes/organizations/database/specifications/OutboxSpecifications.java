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

public class OutboxSpecifications {

  public static final String FIELD_CREATED_AT = "createdAt";
  public static final String FIELD_EVENT_TYPE = "eventType";
  public static final String FIELD_RETRY_COUNT = "retryCount";
  public static final String FIELD_STATUS = "status";
  public static final String FIELD_UPDATED_AT = "updatedAt";

  public static final Set<String> ALLOWED_SORT_FIELDS =
      Set.of(FIELD_CREATED_AT, FIELD_EVENT_TYPE, FIELD_RETRY_COUNT, FIELD_STATUS, FIELD_UPDATED_AT);

  private OutboxSpecifications() {}

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

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_CREATED_AT,
          filter.getCreatedAtFrom(),
          filter.getCreatedAtTo());

      FilterUtilities.addDateRange(
          predicates,
          root,
          query,
          criteriaBuilder,
          FIELD_UPDATED_AT,
          filter.getUpdatedAtFrom(),
          filter.getUpdatedAtTo());

      if (filter.getStatus() != null) {
        predicates.add(hasStatus(filter.getStatus()).toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getMinRetryCount() != null) {
        predicates.add(
            hasMinRetryCount(filter.getMinRetryCount()).toPredicate(root, query, criteriaBuilder));
      }

      if (filter.getMaxRetryCount() != null) {
        predicates.add(
            hasMaxRetryCount(filter.getMaxRetryCount()).toPredicate(root, query, criteriaBuilder));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }

  public static Specification<Outbox> hasEventType(String eventType) {
    return FilterUtilities.likeIgnoreCase(FIELD_EVENT_TYPE, eventType);
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
