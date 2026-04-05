package com.ebikes.organizations.publishers;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.dtos.events.outgoing.AuditEvent;
import com.ebikes.organizations.services.events.OutboxService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {

  private final OutboxService outboxService;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void publishFailure(AuditEvent event, String routingKey) {
    outboxService.save(event.eventType(), event, routingKey);
  }

  public void publishSuccess(AuditEvent event, String routingKey) {
    outboxService.save(event.eventType(), event, routingKey);
  }
}
