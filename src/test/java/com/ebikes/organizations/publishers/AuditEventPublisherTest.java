package com.ebikes.organizations.publishers;

import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.dtos.events.outgoing.AuditEvent;
import com.ebikes.organizations.enums.AuditOutcome;
import com.ebikes.organizations.services.events.OutboxService;

@DisplayName("AuditEventPublisher")
@ExtendWith(MockitoExtension.class)
class AuditEventPublisherTest {

  private static final String EVENT_TYPE = "organizations.organization.created";
  private static final String ROUTING_KEY = "organizations.organization.audit";

  @Mock private OutboxService outboxService;

  @InjectMocks private AuditEventPublisher publisher;

  private AuditEvent event() {
    return new AuditEvent(
        UUID.randomUUID(),
        "ORGANIZATION",
        EVENT_TYPE,
        null,
        null,
        Map.of("status", "ACTIVE"),
        UUID.randomUUID().toString(),
        AuditOutcome.SUCCESS,
        null,
        null,
        UUID.randomUUID().toString());
  }

  @Nested
  @DisplayName("publishSuccess")
  class PublishSuccess {

    @Test
    @DisplayName("should delegate to OutboxService with event type and routing key")
    void shouldDelegateToOutboxService() {
      AuditEvent auditEvent = event();

      publisher.publishSuccess(auditEvent, ROUTING_KEY);

      verify(outboxService).save(EVENT_TYPE, auditEvent, ROUTING_KEY);
    }
  }

  @Nested
  @DisplayName("publishFailure")
  class PublishFailure {

    @Test
    @DisplayName("should delegate to OutboxService with event type and routing key")
    void shouldDelegateToOutboxService() {
      AuditEvent auditEvent = event();

      publisher.publishFailure(auditEvent, ROUTING_KEY);

      verify(outboxService).save(EVENT_TYPE, auditEvent, ROUTING_KEY);
    }
  }
}
