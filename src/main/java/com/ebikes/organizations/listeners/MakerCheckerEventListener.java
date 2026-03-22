package com.ebikes.organizations.listeners;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.EventConstants.RoutingKeys;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.services.InboxService;
import com.ebikes.organizations.services.OrganizationService;
import com.ebikes.organizations.support.context.EventContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class MakerCheckerEventListener implements IncomingEventHandler {

  private final InboxService inboxService;
  private final ObjectMapper objectMapper;
  private final OrganizationService organizationService;

  @Override
  @Transactional
  public void handle(byte[] payload) {
    MakerCheckerDecision event = objectMapper.readValue(payload, MakerCheckerDecision.class);
    log.debug(
        "Received maker-checker decision: entityId={}, entityType={}, operation={}, outcome={}",
        event.entityId(),
        event.entityType(),
        event.operation(),
        event.outcome());

    if (EventContext.absent()) {
      log.warn("No event context found, skipping event processing.");
      return;
    }

    if (!inboxService.receive(
        EventContext.getEventType(), event.serviceReference(), EventContext.getSourceService())) {
      return;
    }
    try {
      organizationService.handleApprovalDecision(event);
      inboxService.markProcessed(event.serviceReference());
    } catch (Exception e) {
      log.error(
          "Failed to process MakerCheckerDecision: serviceReference={}",
          event.serviceReference(),
          e);
    }
  }

  @Override
  public boolean matches(String routingKey) {
    return routingKey.startsWith(RoutingKeys.MAKER_CHECKER_ORGANIZATION);
  }
}
