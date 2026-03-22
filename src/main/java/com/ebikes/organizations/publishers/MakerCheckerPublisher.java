package com.ebikes.organizations.publishers;

import org.springframework.stereotype.Component;

import com.ebikes.organizations.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.organizations.services.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MakerCheckerPublisher {

  private final OutboxService outboxService;

  public void publish(MakerCheckerRequest request, String routingKey) {
    log.debug(
        "Submitting maker-checker request: entityType={}, entityId={}, makerId={}",
        request.entityType(),
        request.entityId(),
        request.makerId());

    outboxService.save(request.entityType(), request, routingKey);

    log.info(
        "Maker-checker request queued: entityType={}, entityId={}",
        request.entityType(),
        request.entityId());
  }
}
