package com.ebikes.organizations.support.makerchecker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import com.ebikes.organizations.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.FieldType;
import com.ebikes.organizations.publishers.MakerCheckerRequestPublisher;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.WithExecutionContext;

@DisplayName("MakerCheckerTemplate")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class MakerCheckerTemplateTest {

  private static final String ORGANIZATION_ID = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String OPERATION = "CREATE";

  @Mock private MakerCheckerRequestPublisher makerCheckerRequestPublisher;

  @InjectMocks private MakerCheckerTemplate template;

  private TestEntity entity() {
    TestEntity entity = new TestEntity();
    ReflectionTestUtils.setField(entity, "id", UUID.randomUUID());
    return entity;
  }

  private FieldChange fieldChange() {
    return new FieldChange("legalName", FieldType.STRING, "New Name", "Old Name");
  }

  @Nested
  @DisplayName("publish without extraContext")
  class PublishWithoutExtraContext {

    @Test
    @DisplayName("should derive entityType from class simple name uppercased")
    void shouldDeriveEntityTypeFromClassName() {
      TestEntity entity = entity();

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of());

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().entityType()).isEqualTo("TESTENTITY");
    }

    @Test
    @DisplayName("should set entityId from entity getId")
    void shouldSetEntityIdFromEntity() {
      TestEntity entity = entity();

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of());

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().entityId()).isEqualTo(entity.getId());
    }

    @Test
    @DisplayName("should set makerId from ExecutionContext userId")
    void shouldSetMakerIdFromExecutionContext() {
      TestEntity entity = entity();

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of());

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().makerId()).isEqualTo(SecurityFixtures.TEST_USER_ID);
    }

    @Test
    @DisplayName("should set operationContext containing only the operation key")
    void shouldSetOperationContextWithOperationKey() {
      TestEntity entity = entity();

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of());

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().operationContext())
          .containsOnly(Map.entry("operation", OPERATION));
    }

    @Test
    @DisplayName("should forward changes to the request")
    void shouldForwardChanges() {
      TestEntity entity = entity();
      List<FieldChange> changes = List.of(fieldChange());

      template.publish(entity, ORGANIZATION_ID, OPERATION, changes);

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().changes()).isEqualTo(changes);
    }

    @Test
    @DisplayName("should publish to routing key derived from entity type")
    void shouldPublishToCorrectRoutingKey() {
      template.publish(entity(), ORGANIZATION_ID, OPERATION, List.of());

      ArgumentCaptor<String> routingKeyCaptor = forClass(String.class);
      verify(makerCheckerRequestPublisher).publish(any(), routingKeyCaptor.capture());

      assertThat(routingKeyCaptor.getValue())
          .isEqualTo("organizations.testentity.maker-checker-request");
    }
  }

  @Nested
  @DisplayName("publish with extraContext")
  class PublishWithExtraContext {

    @Test
    @DisplayName("should merge extraContext into operationContext alongside operation key")
    void shouldMergeExtraContextWithOperationKey() {
      TestEntity entity = entity();
      Map<String, Object> extraContext = Map.of("replacedDocumentId", "doc-123");

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of(), extraContext);

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().operationContext())
          .containsEntry("operation", OPERATION)
          .containsEntry("replacedDocumentId", "doc-123");
    }

    @Test
    @DisplayName("operation key should not be overridable by extraContext")
    void operationKeyShouldNotBeOverridableByExtraContext() {
      TestEntity entity = entity();
      Map<String, Object> extraContext = Map.of("operation", "SHOULD_NOT_WIN");

      template.publish(entity, ORGANIZATION_ID, OPERATION, List.of(), extraContext);

      ArgumentCaptor<MakerCheckerRequest> captor = forClass(MakerCheckerRequest.class);
      verify(makerCheckerRequestPublisher).publish(captor.capture(), any());

      assertThat(captor.getValue().operationContext()).containsEntry("operation", OPERATION);
    }
  }

  private static final class TestEntity extends BaseEntity {}
}
