package com.ebikes.organizations.support.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.exceptions.ValidationException;

@DisplayName("FilterUtilities")
class FilterUtilitiesTest {

  private static final Set<String> ALLOWED_FIELDS = Set.of("createdAt", "status", "eventType");

  @Test
  @DisplayName("should return correct Pageable for valid sort field")
  void shouldReturnCorrectPageable() {
    OutboxFilter filter = new OutboxFilter();
    filter.setSortBy("status");
    filter.setSortDirection("ASC");
    filter.setPage(1);
    filter.setSize(10);

    Pageable pageable = FilterUtilities.buildPageable(filter, ALLOWED_FIELDS);

    assertThat(pageable.getPageSize()).isEqualTo(10);
    assertThat(Objects.requireNonNull(pageable.getSort().getOrderFor("status")).getDirection())
        .isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("should throw ValidationException when sortBy is not in allowed fields")
  void shouldThrowWhenSortFieldInvalid() {
    OutboxFilter filter = new OutboxFilter();
    filter.setSortBy("invalidField");

    assertThatThrownBy(() -> FilterUtilities.buildPageable(filter, ALLOWED_FIELDS))
        .isInstanceOf(ValidationException.class);
  }

  @Test
  @DisplayName("should correctly convert 1-based page to 0-based index")
  void shouldConvertPageToZeroBased() {
    OutboxFilter filter = new OutboxFilter();
    filter.setSortBy("createdAt");
    filter.setPage(3);
    filter.setSize(20);

    Pageable pageable = FilterUtilities.buildPageable(filter, ALLOWED_FIELDS);

    assertThat(pageable.getPageNumber()).isEqualTo(2);
  }
}
