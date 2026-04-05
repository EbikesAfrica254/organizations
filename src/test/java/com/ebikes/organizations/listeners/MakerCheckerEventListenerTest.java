package com.ebikes.organizations.listeners;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.constants.EventConstants.ExternalContracts;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.services.events.InboxService;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.support.context.EventContext;
import com.ebikes.organizations.support.fixtures.MakerCheckerFixtures;

import tools.jackson.databind.ObjectMapper;

@DisplayName("MakerCheckerEventListener")
@ExtendWith(MockitoExtension.class)
class MakerCheckerEventListenerTest {

  private static final byte[] PAYLOAD = "{}".getBytes();
  private static final String EVENT_TYPE = "maker-checker.decision";
  private static final String SOURCE_SERVICE = "iam";

  @Mock private InboxService inboxService;
  @Mock private ObjectMapper objectMapper;
  @Mock private OrganizationService organizationService;

  private MakerCheckerEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new MakerCheckerEventListener(inboxService, objectMapper, organizationService);
    EventContext.set(UUID.randomUUID().toString(), EVENT_TYPE, "test.routing.key", SOURCE_SERVICE);
  }

  @AfterEach
  void tearDown() {
    EventContext.clear();
  }

  @Test
  @DisplayName("should skip processing when event context is absent")
  void shouldSkipProcessingWhenEventContextIsAbsent() {
    EventContext.clear();
    MakerCheckerDecision event =
        MakerCheckerFixtures.approved(UUID.randomUUID(), "ORGANIZATION", "CREATE");
    when(objectMapper.readValue(PAYLOAD, MakerCheckerDecision.class)).thenReturn(event);

    listener.handle(PAYLOAD);

    verify(inboxService, never()).receive(any(), any(), any());
    verify(organizationService, never()).handleApprovalDecision(any());
  }

  @Test
  @DisplayName("should skip processing when inbox rejects duplicate event")
  void shouldSkipProcessingWhenInboxRejectsDuplicateEvent() {
    MakerCheckerDecision event =
        MakerCheckerFixtures.approved(UUID.randomUUID(), "ORGANIZATION", "CREATE");
    when(objectMapper.readValue(PAYLOAD, MakerCheckerDecision.class)).thenReturn(event);
    when(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE_SERVICE))
        .thenReturn(false);

    listener.handle(PAYLOAD);

    verify(organizationService, never()).handleApprovalDecision(any());
    verify(inboxService, never()).markProcessed(any());
  }

  @Test
  @DisplayName("should process event and mark processed when inbox accepts event")
  void shouldProcessEventWhenInboxAcceptsEvent() {
    MakerCheckerDecision event =
        MakerCheckerFixtures.approved(UUID.randomUUID(), "ORGANIZATION", "CREATE");
    when(objectMapper.readValue(PAYLOAD, MakerCheckerDecision.class)).thenReturn(event);
    when(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE_SERVICE))
        .thenReturn(true);

    listener.handle(PAYLOAD);

    verify(organizationService).handleApprovalDecision(event);
    verify(inboxService).markProcessed(event.serviceReference());
  }

  @Test
  @DisplayName("should not mark processed when handling throws")
  void shouldNotMarkProcessedWhenHandlingFails() {
    MakerCheckerDecision event =
        MakerCheckerFixtures.approved(UUID.randomUUID(), "ORGANIZATION", "CREATE");
    when(objectMapper.readValue(PAYLOAD, MakerCheckerDecision.class)).thenReturn(event);
    when(inboxService.receive(EVENT_TYPE, event.serviceReference(), SOURCE_SERVICE))
        .thenReturn(true);
    org.mockito.Mockito.doThrow(new RuntimeException("processing failed"))
        .when(organizationService)
        .handleApprovalDecision(any());

    assertThatNoException().isThrownBy(() -> listener.handle(PAYLOAD));
    verify(inboxService, never()).markProcessed(any());
  }

  @Test
  @DisplayName("should match routing key starting with maker-checker organization prefix")
  void shouldMatchMakerCheckerOrganizationRoutingKey() {
    assertThat(listener.matches(ExternalContracts.MAKER_CHECKER_ORGANIZATION + ".approved"))
        .isTrue();
  }

  @Test
  @DisplayName("should not match unrelated routing key")
  void shouldNotMatchUnrelatedRoutingKey() {
    assertThat(listener.matches("organizations.organization.created")).isFalse();
  }
}
