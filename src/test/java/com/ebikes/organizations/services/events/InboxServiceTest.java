package com.ebikes.organizations.services.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.database.entities.Inbox;
import com.ebikes.organizations.database.repositories.InboxRepository;

@DisplayName("InboxService")
@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

  private static final String SERVICE_REFERENCE = UUID.randomUUID().toString();
  private static final String EVENT_TYPE = "USER_CREATED";
  private static final String SOURCE_CONTEXT = "organizations-service";

  @Mock private InboxRepository inboxRepository;

  private InboxService service;

  @BeforeEach
  void setUp() {
    service = new InboxService(inboxRepository);
  }

  @Nested
  @DisplayName("markProcessed")
  class MarkProcessed {

    @Test
    @DisplayName("should mark inbox as processed and publish")
    void shouldMarkProcessedAndSave() {
      Inbox inbox = new Inbox(EVENT_TYPE, SERVICE_REFERENCE, SOURCE_CONTEXT);
      when(inboxRepository.findById(SERVICE_REFERENCE)).thenReturn(Optional.of(inbox));

      service.markProcessed(SERVICE_REFERENCE);

      assertThat(inbox.getProcessedAt()).isNotNull();
      verify(inboxRepository).save(inbox);
    }

    @Test
    @DisplayName("should throw IllegalStateException when inbox record not found")
    void shouldThrowWhenNotFound() {
      when(inboxRepository.findById(SERVICE_REFERENCE)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.markProcessed(SERVICE_REFERENCE))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining(SERVICE_REFERENCE);
    }
  }

  @Nested
  @DisplayName("receive")
  class Receive {

    @Test
    @DisplayName("should publish inbox record and return true")
    void shouldSaveAndReturnTrue() {
      when(inboxRepository.save(any(Inbox.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      boolean result = service.receive(EVENT_TYPE, SERVICE_REFERENCE, SOURCE_CONTEXT);

      assertThat(result).isTrue();
      verify(inboxRepository).save(any(Inbox.class));
    }

    @Test
    @DisplayName("should return false on duplicate event")
    void shouldReturnFalseOnDuplicate() {
      when(inboxRepository.existsById(SERVICE_REFERENCE)).thenReturn(true);

      boolean result = service.receive(EVENT_TYPE, SERVICE_REFERENCE, SOURCE_CONTEXT);

      assertThat(result).isFalse();
      verify(inboxRepository, never()).save(any());
    }
  }
}
