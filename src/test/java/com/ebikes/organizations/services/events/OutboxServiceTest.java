package com.ebikes.organizations.services.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.dtos.responses.api.PaginatedResponse;
import com.ebikes.organizations.dtos.responses.outbox.OutboxResponse;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.mappers.OutboxMapper;
import com.ebikes.organizations.support.fixtures.EventFixtures;
import com.ebikes.organizations.support.fixtures.OutboxFixtures;

@DisplayName("OutboxService")
@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

  private static final UUID OUTBOX_ID = UUID.randomUUID();

  @Mock private OutboxMapper mapper;
  @Mock private OutboxRepository repository;

  private OutboxService service;

  @BeforeEach
  void setUp() {
    service = new OutboxService(mapper, repository);
  }

  private OutboxResponse outboxResponse(Outbox outbox) {
    return new OutboxResponse(
        OUTBOX_ID,
        OffsetDateTime.now(),
        outbox.getEventType(),
        outbox.getRetryCount(),
        outbox.getRoutingKey(),
        outbox.getStatus(),
        null);
  }

  @Nested
  @DisplayName("retry")
  class Retry {

    @Test
    @DisplayName("should reset outbox to PENDING and publish")
    void shouldResetToPendingAndSave() {
      Outbox outbox = OutboxFixtures.failed("ORGANIZATION_CREATED");
      when(repository.findById(OUTBOX_ID)).thenReturn(Optional.of(outbox));

      service.retry(OUTBOX_ID);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
      verify(repository).save(outbox);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(OUTBOX_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.retry(OUTBOX_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("retryAllFailed")
  class RetryAllFailed {

    @Test
    @DisplayName("should reset all failed events to PENDING and publish")
    void shouldResetAllFailedAndSave() {
      Outbox first = OutboxFixtures.failed("ORGANIZATION_CREATED");
      Outbox second = OutboxFixtures.failed("USER_UPDATED");
      when(repository.findByStatusOrderByIdAsc(OutboxStatus.FAILED))
          .thenReturn(List.of(first, second));

      service.retryAllFailed();

      assertThat(first.getStatus()).isEqualTo(OutboxStatus.PENDING);
      assertThat(second.getStatus()).isEqualTo(OutboxStatus.PENDING);
      verify(repository).saveAll(List.of(first, second));
    }

    @Test
    @DisplayName("should return count of reset events")
    void shouldReturnCountOfResetEvents() {
      when(repository.findByStatusOrderByIdAsc(OutboxStatus.FAILED))
          .thenReturn(
              List.of(
                  OutboxFixtures.failed("ORGANIZATION_CREATED"),
                  OutboxFixtures.failed("ORGANIZATION_UPDATED")));

      assertThat(service.retryAllFailed()).isEqualTo(2);
    }
  }

  @Nested
  @DisplayName("publish")
  class Save {

    @Test
    @DisplayName("should build and persist outbox with correct fields")
    void shouldBuildAndPersistOutbox() {
      service.publish(
          "ORGANIZATION_CREATED", EventFixtures.organizationCreatedEvent(), "test.routing.key");

      verify(repository).save(any(Outbox.class));
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should build spec and pageable and return paginated response")
    @SuppressWarnings("unchecked")
    void shouldReturnPaginatedResponse() {
      Outbox outbox = OutboxFixtures.pending("ORGANIZATION_CREATED");
      OutboxResponse response = outboxResponse(outbox);
      when(repository.findAll(any(Specification.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(List.of(outbox)));
      when(mapper.toResponse(outbox)).thenReturn(response);

      PaginatedResponse<OutboxResponse> result = service.search(new OutboxFilter());

      assertThat(result.data()).containsExactly(response);
    }
  }

  @Nested
  @DisplayName("requireById")
  class RequireById {

    @Test
    @DisplayName("should return outbox when found")
    void shouldReturnOutboxWhenFound() {
      Outbox outbox = OutboxFixtures.pending("ORGANIZATION_CREATED");
      when(repository.findById(OUTBOX_ID)).thenReturn(Optional.of(outbox));

      assertThat(service.requireById(OUTBOX_ID)).isEqualTo(outbox);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(OUTBOX_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.requireById(OUTBOX_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
