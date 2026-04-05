package com.ebikes.organizations.database.specifications;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.support.fixtures.EventFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractRepositoryTest;

@DisplayName("OutboxSpecifications")
class OutboxSpecificationsTest extends AbstractRepositoryTest {

  @Autowired private OutboxRepository repository;

  private static final String EVENT_ORGANIZATION_CREATED = "ORGANIZATION_CREATED";
  private static final String EVENT_ORGANIZATION_UPDATED = "ORGANIZATION_UPDATED";

  private Outbox pending(String eventType) {
    return Outbox.builder()
        .eventType(eventType)
        .payload(EventFixtures.organizationCreatedEvent())
        .routingKey("test.routing.key")
        .build();
  }

  private Outbox failed() {
    Outbox outbox = pending(OutboxSpecificationsTest.EVENT_ORGANIZATION_CREATED);
    outbox.markFailed();
    return outbox;
  }

  private Outbox sent() {
    Outbox outbox = pending(OutboxSpecificationsTest.EVENT_ORGANIZATION_UPDATED);
    outbox.markSent();
    return outbox;
  }

  private Outbox failedWithRetries() {
    Outbox outbox = pending(OutboxSpecificationsTest.EVENT_ORGANIZATION_CREATED);
    for (int i = 0; i < 2; i++) {
      outbox.markFailed();
      outbox.resetForRetry();
    }
    outbox.markFailed();
    return outbox;
  }

  @BeforeEach
  void setUp() {
    repository.saveAll(
        List.of(
            pending(EVENT_ORGANIZATION_CREATED),
            pending(EVENT_ORGANIZATION_UPDATED),
            failed(),
            sent(),
            failedWithRetries()));
  }

  @AfterEach
  void tearDown() {
    repository.deleteAll();
  }

  @Nested
  @DisplayName("Filter by status")
  class FilterByStatus {

    @Test
    @DisplayName("should return only PENDING records")
    void shouldReturnOnlyPendingRecords() {
      OutboxFilter filter = new OutboxFilter();
      filter.setStatus(OutboxStatus.PENDING);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(o -> o.getStatus() == OutboxStatus.PENDING);
    }

    @Test
    @DisplayName("should return only FAILED records")
    void shouldReturnOnlyFailedRecords() {
      OutboxFilter filter = new OutboxFilter();
      filter.setStatus(OutboxStatus.FAILED);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(2).allMatch(o -> o.getStatus() == OutboxStatus.FAILED);
    }

    @Test
    @DisplayName("should return only SENT records")
    void shouldReturnOnlySentRecords() {
      OutboxFilter filter = new OutboxFilter();
      filter.setStatus(OutboxStatus.SENT);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(o -> o.getStatus() == OutboxStatus.SENT);
    }
  }

  @Nested
  @DisplayName("Filter by event type")
  class FilterByEventType {

    @Test
    @DisplayName("should return only matching event type")
    void shouldReturnOnlyMatchingEventType() {
      OutboxFilter filter = new OutboxFilter();
      filter.setEventType(EVENT_ORGANIZATION_CREATED);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(3)
          .allMatch(o -> o.getEventType().equals(EVENT_ORGANIZATION_CREATED));
    }

    @Test
    @DisplayName("should return nothing for unknown event type")
    void shouldReturnNothingForUnknownEventType() {
      OutboxFilter filter = new OutboxFilter();
      filter.setEventType("UNKNOWN_EVENT");

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }
  }

  @Nested
  @DisplayName("Filter by retry count")
  class FilterByRetryCount {

    @Test
    @DisplayName("should return records with retryCount >= minRetryCount")
    void shouldReturnRecordsWithRetryCountAtLeastMin() {
      OutboxFilter filter = new OutboxFilter();
      filter.setMinRetryCount(2);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(1).allMatch(o -> o.getRetryCount() >= 2);
    }

    @Test
    @DisplayName("should return records with retryCount <= maxRetryCount")
    void shouldReturnRecordsWithRetryCountAtMostMax() {
      OutboxFilter filter = new OutboxFilter();
      filter.setMaxRetryCount(1);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(4).allMatch(o -> o.getRetryCount() <= 1);
    }

    @Test
    @DisplayName("should return records within min and max range combined")
    void shouldReturnRecordsWithinMinAndMaxRange() {
      OutboxFilter filter = new OutboxFilter();
      filter.setMinRetryCount(1);
      filter.setMaxRetryCount(2);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(1)
          .allMatch(o -> o.getRetryCount() >= 1 && o.getRetryCount() <= 2);
    }
  }

  @Nested
  @DisplayName("Filter by date range")
  class FilterByDateRange {

    @Test
    @DisplayName("createdAtFrom filters out records created before the threshold")
    void createdAtFromFiltersOldRecords() {
      OutboxFilter filter = new OutboxFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtTo filters out records created after the threshold")
    void createdAtToFiltersNewRecords() {
      OutboxFilter filter = new OutboxFilter();
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("createdAtFrom and createdAtTo combined returns records within range")
    void createdAtFromAndToReturnsRecordsWithinRange() {
      OutboxFilter filter = new OutboxFilter();
      filter.setCreatedAtFrom(OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(1));
      filter.setCreatedAtTo(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1));

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results).hasSize(5);
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("status and eventType combined returns correct subset")
    void statusAndEventTypeCombinedReturnsCorrectSubset() {
      OutboxFilter filter = new OutboxFilter();
      filter.setStatus(OutboxStatus.FAILED);
      filter.setEventType(EVENT_ORGANIZATION_CREATED);

      List<Outbox> results = repository.findAll(OutboxSpecifications.buildSpecification(filter));

      assertThat(results)
          .hasSize(2)
          .first()
          .satisfies(
              o -> {
                assertThat(o.getEventType()).isEqualTo(EVENT_ORGANIZATION_CREATED);
                assertThat(o.getStatus()).isEqualTo(OutboxStatus.FAILED);
              });
    }
  }
}
