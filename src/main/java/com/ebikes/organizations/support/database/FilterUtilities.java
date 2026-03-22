package com.ebikes.organizations.support.database;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.dtos.requests.filters.BaseFilter;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ValidationException;

import lombok.experimental.UtilityClass;

@UtilityClass
public class FilterUtilities {

  public static Pageable buildPageable(BaseFilter filter, Set<String> allowedSortFields) {
    String sortBy = filter.getSortBy();
    if (!allowedSortFields.contains(sortBy)) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Invalid sort field specified. Allowed fields: " + allowedSortFields,
          "sortBy",
          sortBy);
    }
    Sort.Direction direction = Sort.Direction.fromString(filter.getSortDirection());
    Sort sort = Sort.by(direction, sortBy);
    int zeroIndexedPage = filter.getPage() - 1;
    return PageRequest.of(zeroIndexedPage, filter.getSize(), sort);
  }

  public static <T> void addDateRange(
      List<Predicate> predicates,
      Root<T> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      String field,
      OffsetDateTime from,
      OffsetDateTime to) {
    if (from != null || to != null) {
      predicates.add(
          FilterUtilities.<T>dateRangeBetween(field, from, to)
              .toPredicate(root, query, criteriaBuilder));
    }
  }

  public static <T> void addIfPresent(
      List<Predicate> predicates,
      Root<T> root,
      CriteriaQuery<?> query,
      CriteriaBuilder criteriaBuilder,
      Object value,
      Specification<T> spec) {
    if (value != null && !(value instanceof String s && s.isBlank())) {
      predicates.add(spec.toPredicate(root, query, criteriaBuilder));
    }
  }

  public static <T> Specification<T> dateRangeBetween(
      String fieldPath, OffsetDateTime from, OffsetDateTime to) {
    return (root, query, criteriaBuilder) -> {
      if (from != null && to != null) {
        return criteriaBuilder.between(root.get(fieldPath), from, to);
      } else if (from != null) {
        return criteriaBuilder.greaterThanOrEqualTo(root.get(fieldPath), from);
      } else if (to != null) {
        return criteriaBuilder.lessThanOrEqualTo(root.get(fieldPath), to);
      }
      return criteriaBuilder.conjunction();
    };
  }

  public static <T> Specification<T> likeIgnoreCase(String fieldPath, String searchTerm) {
    return (root, query, criteriaBuilder) -> {
      String pattern = "%" + searchTerm.toLowerCase().trim() + "%";
      return criteriaBuilder.like(criteriaBuilder.lower(root.get(fieldPath)), pattern);
    };
  }
}
