package com.ebikes.organizations.publishers;

import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.organizations.services.events.OutboxService;

@DisplayName("MakerCheckerRequestPublisher")
@ExtendWith(MockitoExtension.class)
class MakerCheckerRequestPublisherTest {

  private static final String ENTITY_TYPE = "ORGANIZATION";
  private static final String ROUTING_KEY = "organizations.organization.maker-checker-request";
  private static final String ORGANIZATION_ID = UUID.randomUUID().toString();

  @Mock private OutboxService outboxService;

  @InjectMocks private MakerCheckerRequestPublisher publisher;

  private MakerCheckerRequest request() {
    return new MakerCheckerRequest(
        null,
        List.of(),
        UUID.randomUUID(),
        ENTITY_TYPE,
        UUID.randomUUID().toString(),
        Map.of("operation", "CREATE"),
        ORGANIZATION_ID,
        Instant.now(),
        null);
  }

  @Test
  @DisplayName(
      "should delegate to OutboxService using entityType as event type and correct routing key")
  void shouldDelegateToOutboxServiceWithCorrectArguments() {
    MakerCheckerRequest makerCheckerRequest = request();

    publisher.publish(makerCheckerRequest, ROUTING_KEY);

    verify(outboxService).save(ENTITY_TYPE, makerCheckerRequest, ROUTING_KEY);
  }
}
