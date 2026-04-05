package com.ebikes.organizations.integration.services.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ebikes.organizations.database.entities.Inbox;
import com.ebikes.organizations.database.repositories.InboxRepository;
import com.ebikes.organizations.services.events.InboxService;
import com.ebikes.organizations.support.infrastructure.AbstractIntegrationTest;

class InboxServiceIT extends AbstractIntegrationTest {

  @Autowired private InboxService inboxService;
  @Autowired private InboxRepository inboxRepository;

  @BeforeEach
  void setUp() {
    inboxRepository.deleteAll();
  }

  @Nested
  class Receive {

    @Test
    @DisplayName("returns true and persists record for new event")
    void returnsTrueAndPersistsRecordForNewEvent() {
      boolean result = inboxService.receive("organization.branch.created", "ref-001", "orders");

      assertThat(result).isTrue();
      assertThat(inboxRepository.findAll())
          .hasSize(1)
          .first()
          .satisfies(
              inbox -> {
                assertThat(inbox.getEventType()).isEqualTo("organization.branch.created");
                assertThat(inbox.getSourceContext()).isEqualTo("orders");
                assertThat(inbox.getReceivedAt()).isNotNull();
                assertThat(inbox.getProcessedAt()).isNull();
              });
    }

    @Test
    @DisplayName("returns false for duplicate service reference")
    void returnsFalseForDuplicateServiceReference() {
      inboxService.receive("organization.branch.created", "ref-001", "orders");

      boolean result = inboxService.receive("organization.branch.created", "ref-001", "orders");

      assertThat(result).isFalse();
      assertThat(inboxRepository.findAll()).hasSize(1);
    }
  }

  @Nested
  class MarkProcessed {

    @Test
    @DisplayName("sets processedAt")
    void setsProcessedAt() {
      inboxRepository.save(new Inbox("organization.branch.created", "ref-001", "orders"));

      inboxService.markProcessed("ref-001");

      assertThat(inboxRepository.findById("ref-001"))
          .isPresent()
          .get()
          .satisfies(inbox -> assertThat(inbox.getProcessedAt()).isNotNull());
    }

    @Test
    @DisplayName("throws when already processed")
    void throwsWhenAlreadyProcessed() {
      Inbox inbox = new Inbox("organization.branch.created", "ref-001", "orders");
      inbox.markProcessed();
      inboxRepository.save(inbox);

      assertThatThrownBy(() -> inboxService.markProcessed("ref-001"))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("throws when service reference not found")
    void throwsWhenServiceReferenceNotFound() {
      assertThatThrownBy(() -> inboxService.markProcessed("non-existent"))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
