package com.ebikes.organizations.support.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.database.entities.bases.BaseEntity;
import com.ebikes.organizations.dtos.events.outgoing.AuditEvent;
import com.ebikes.organizations.enums.AuditOutcome;
import com.ebikes.organizations.publishers.AuditEventPublisher;
import com.ebikes.organizations.support.infrastructure.WithExecutionContext;

@DisplayName("AuditTemplate")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class AuditTemplateTest {

  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();
  private static final String EVENT_TYPE = "organizations.organization.created";
  private static final String ROUTING_KEY = "organizations.testauditableentity.audit";

  @Mock private AuditEventPublisher auditEventPublisher;

  @InjectMocks private AuditTemplate auditTemplate;

  private TestAuditableEntity entity;

  @BeforeEach
  void setUp() {
    entity = new TestAuditableEntity();
    ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
  }

  @Nested
  @DisplayName("execute with ThrowingRunnable")
  class ExecuteWithRunnable {

    @Test
    @DisplayName("should execute the operation")
    void shouldExecuteTheOperation() {
      boolean[] invoked = {false};

      ThrowingRunnable<Exception> operation = () -> invoked[0] = true;

      auditTemplate.execute(entity, ORGANIZATION_ID, EVENT_TYPE, operation);

      assertThat(invoked[0]).isTrue();
      verify(auditEventPublisher).publishSuccess(any(AuditEvent.class), eq(ROUTING_KEY));
    }

    @Test
    @DisplayName("should publish a success audit event when the operation succeeds")
    void shouldPublishSuccessEventOnSuccess() {
      auditTemplate.execute(entity, ORGANIZATION_ID, EVENT_TYPE, () -> {});

      ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

      verify(auditEventPublisher).publishSuccess(eventCaptor.capture(), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishFailure(any(), any());

      AuditEvent event = eventCaptor.getValue();
      assertThat(event.entityId()).isEqualTo(entity.getId());
      assertThat(event.entityType()).isEqualTo("TESTAUDITABLEENTITY");
      assertThat(event.eventType()).isEqualTo(EVENT_TYPE);
      assertThat(event.organizationId()).isEqualTo(ORGANIZATION_ID);
      assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
      assertThat(event.failureReason()).isNull();
      assertThat(event.metadata()).isEqualTo(entity.toAuditMetadata());
    }

    @Test
    @DisplayName("should publish a failure audit event and rethrow when the operation throws")
    void shouldPublishFailureEventAndRethrowOnException() {
      RuntimeException cause = new RuntimeException("operation failed");

      assertThatThrownBy(
              () ->
                  auditTemplate.execute(
                      entity,
                      ORGANIZATION_ID,
                      EVENT_TYPE,
                      () -> {
                        throw cause;
                      }))
          .isSameAs(cause);

      ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

      verify(auditEventPublisher).publishFailure(eventCaptor.capture(), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishSuccess(any(), any());

      AuditEvent event = eventCaptor.getValue();
      assertThat(event.entityId()).isEqualTo(entity.getId());
      assertThat(event.entityType()).isEqualTo("TESTAUDITABLEENTITY");
      assertThat(event.eventType()).isEqualTo(EVENT_TYPE);
      assertThat(event.organizationId()).isEqualTo(ORGANIZATION_ID);
      assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
      assertThat(event.failureReason()).isEqualTo("operation failed");
      assertThat(event.metadata()).isEqualTo(entity.toAuditMetadata());
    }
  }

  @Nested
  @DisplayName("execute with ThrowingSupplier")
  class ExecuteWithSupplier {

    @Test
    @DisplayName("should return the result of the operation")
    void shouldReturnOperationResult() {
      TestAuditableEntity result =
          auditTemplate.execute(entity, ORGANIZATION_ID, EVENT_TYPE, () -> entity);

      assertThat(result).isSameAs(entity);
    }

    @Test
    @DisplayName("should publish a success audit event when the operation succeeds")
    void shouldPublishSuccessEventOnSuccess() {
      auditTemplate.execute(entity, ORGANIZATION_ID, EVENT_TYPE, () -> entity);

      ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

      verify(auditEventPublisher).publishSuccess(eventCaptor.capture(), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishFailure(any(), any());

      AuditEvent event = eventCaptor.getValue();
      assertThat(event.entityId()).isEqualTo(entity.getId());
      assertThat(event.entityType()).isEqualTo("TESTAUDITABLEENTITY");
      assertThat(event.eventType()).isEqualTo(EVENT_TYPE);
      assertThat(event.organizationId()).isEqualTo(ORGANIZATION_ID);
      assertThat(event.outcome()).isEqualTo(AuditOutcome.SUCCESS);
      assertThat(event.failureReason()).isNull();
      assertThat(event.metadata()).isEqualTo(entity.toAuditMetadata());
    }

    @Test
    @DisplayName("should publish a failure audit event and rethrow when the operation throws")
    void shouldPublishFailureEventAndRethrowOnException() {
      RuntimeException cause = new RuntimeException("supplier failed");

      assertThatThrownBy(
              () ->
                  auditTemplate.execute(
                      entity,
                      ORGANIZATION_ID,
                      EVENT_TYPE,
                      () -> {
                        throw cause;
                      }))
          .isSameAs(cause);

      ArgumentCaptor<AuditEvent> eventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

      verify(auditEventPublisher).publishFailure(eventCaptor.capture(), eq(ROUTING_KEY));
      verify(auditEventPublisher, never()).publishSuccess(any(), any());

      AuditEvent event = eventCaptor.getValue();
      assertThat(event.entityId()).isEqualTo(entity.getId());
      assertThat(event.entityType()).isEqualTo("TESTAUDITABLEENTITY");
      assertThat(event.eventType()).isEqualTo(EVENT_TYPE);
      assertThat(event.organizationId()).isEqualTo(ORGANIZATION_ID);
      assertThat(event.outcome()).isEqualTo(AuditOutcome.FAILURE);
      assertThat(event.failureReason()).isEqualTo("supplier failed");
      assertThat(event.metadata()).isEqualTo(entity.toAuditMetadata());
    }
  }

  private static final class TestAuditableEntity extends BaseEntity implements Auditable {

    @Override
    public Map<String, String> toAuditMetadata() {
      return Map.of("field", "value");
    }
  }
}
