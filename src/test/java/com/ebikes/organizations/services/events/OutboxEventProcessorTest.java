package com.ebikes.organizations.services.events;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.stream.function.StreamBridge;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.support.fixtures.OutboxFixtures;

@DisplayName("OutboxEventProcessor")
@ExtendWith(MockitoExtension.class)
class OutboxEventProcessorTest {

  private static final String EVENT_TYPE = "organizations.organization.created";
  private static final String BINDING_NAME = ApplicationConstants.Outbox.BINDING_NAME;

  @Mock private OutboxRepository repository;
  @Mock private StreamBridge streamBridge;

  private OutboxEventProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new OutboxEventProcessor(repository, streamBridge);
  }

  private Outbox outboxAtMaxRetries() {
    Outbox outbox =
        OutboxFixtures.failedWithRetries(
            EVENT_TYPE, ApplicationConstants.Outbox.MAX_RETRY_COUNT - 2);
    outbox.resetForRetry();
    return outbox;
  }

  @Nested
  @DisplayName("when StreamBridge sends successfully")
  class WhenSendSucceeds {

    @Test
    @DisplayName("should mark outbox as SENT and publish")
    void shouldMarkSentAndSave() {
      Outbox outbox = OutboxFixtures.pending(EVENT_TYPE);
      when(streamBridge.send(eq(BINDING_NAME), any())).thenReturn(true);

      processor.process(outbox);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.SENT);
      verify(repository).save(outbox);
    }

    @Test
    @DisplayName("should not mark outbox as FAILED")
    void shouldNotMarkFailed() {
      Outbox outbox = OutboxFixtures.pending(EVENT_TYPE);
      when(streamBridge.send(eq(BINDING_NAME), any())).thenReturn(true);

      processor.process(outbox);

      assertThat(outbox.getStatus()).isNotEqualTo(OutboxStatus.FAILED);
    }
  }

  @Nested
  @DisplayName("when StreamBridge returns false")
  class WhenSendReturnsFalse {

    @Test
    @DisplayName("should mark outbox as FAILED and publish")
    void shouldMarkFailedAndSave() {
      Outbox outbox = OutboxFixtures.pending(EVENT_TYPE);
      when(streamBridge.send(eq(BINDING_NAME), any())).thenReturn(false);

      processor.process(outbox);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
      assertThat(outbox.getRetryCount()).isEqualTo(1);
      verify(repository).save(outbox);
    }

    @Test
    @DisplayName("should mark as DEAD_LETTER when retry count reaches max")
    void shouldMarkDeadLetterAtMaxRetries() {
      Outbox outbox = outboxAtMaxRetries();
      when(streamBridge.send(eq(BINDING_NAME), any())).thenReturn(false);

      processor.process(outbox);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
      assertThat(outbox.getRetryCount()).isEqualTo(ApplicationConstants.Outbox.MAX_RETRY_COUNT);
      verify(repository).save(outbox);
    }
  }

  @Nested
  @DisplayName("when StreamBridge throws an exception")
  class WhenSendThrows {

    @Test
    @DisplayName("should mark outbox as FAILED and publish")
    void shouldMarkFailedAndSave() {
      Outbox outbox = OutboxFixtures.pending(EVENT_TYPE);
      doThrow(new RuntimeException("broker unavailable"))
          .when(streamBridge)
          .send(eq(BINDING_NAME), any());

      processor.process(outbox);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.FAILED);
      assertThat(outbox.getRetryCount()).isEqualTo(1);
      verify(repository).save(outbox);
    }

    @Test
    @DisplayName("should mark as DEAD_LETTER when retry count reaches max")
    void shouldMarkDeadLetterAtMaxRetries() {
      Outbox outbox = outboxAtMaxRetries();
      doThrow(new RuntimeException("broker unavailable"))
          .when(streamBridge)
          .send(eq(BINDING_NAME), any());

      processor.process(outbox);

      assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.DEAD_LETTER);
      assertThat(outbox.getRetryCount()).isEqualTo(ApplicationConstants.Outbox.MAX_RETRY_COUNT);
      verify(repository).save(outbox);
    }
  }
}
