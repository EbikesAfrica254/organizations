package com.ebikes.organizations.publishers;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.constants.MDCKeys;
import com.ebikes.organizations.dtos.events.outgoing.AuditEvent;
import com.ebikes.organizations.enums.AuditOutcome;
import com.ebikes.organizations.services.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventPublisher {

  private final OutboxService outboxService;

  public void publishSuccess(
      UUID entityId,
      String entityType,
      String eventType,
      Map<String, String> metadata,
      String organizationId,
      String routingKey,
      String userId) {

    var event =
        new AuditEvent(
            entityId,
            entityType,
            eventType,
            null,
            MDC.get(MDCKeys.IP_ADDRESS),
            metadata,
            organizationId,
            AuditOutcome.SUCCESS,
            null,
            null,
            userId);

    outboxService.save(eventType, event, routingKey);
  }
}
