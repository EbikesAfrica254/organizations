package com.ebikes.organizations.publishers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.services.events.OutboxEventProcessor;
import com.ebikes.organizations.support.fixtures.OutboxFixtures;

@DisplayName("OutboxPublisher")
@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest {

  private static final String EVENT_TYPE = "iam.user-extension.created";

  @Mock private OutboxEventProcessor eventProcessor;
  @Mock private OutboxRepository repository;

  private OutboxPublisher publisher;

  @BeforeEach
  void setUp() {
    publisher = new OutboxPublisher(eventProcessor, repository);
  }

  @Test
  @DisplayName("should do nothing when there are no pending events")
  void shouldDoNothingWhenNoPendingEvents() {
    when(repository.findByStatusOrderByIdAsc(OutboxStatus.PENDING)).thenReturn(List.of());

    publisher.publishPendingEvents();

    verify(eventProcessor, never()).process(any());
  }

  @Test
  @DisplayName("should process each pending event")
  void shouldProcessEachPendingEvent() {
    Outbox first = OutboxFixtures.pending(EVENT_TYPE);
    Outbox second = OutboxFixtures.pending(EVENT_TYPE);
    when(repository.findByStatusOrderByIdAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(first, second));

    publisher.publishPendingEvents();

    verify(eventProcessor).process(first);
    verify(eventProcessor).process(second);
  }
}
