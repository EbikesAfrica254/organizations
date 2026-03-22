package com.ebikes.organizations.publishers;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.services.OutboxEventProcessor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

  private final OutboxEventProcessor eventProcessor;
  private final OutboxRepository repository;

  @Scheduled(fixedDelay = 10000)
  public void publishPendingEvents() {
    List<Outbox> pendingEvents = repository.findByStatusOrderByIdAsc(OutboxStatus.PENDING);

    if (pendingEvents.isEmpty()) {
      return;
    }

    log.debug("Processing {} pending outbox events", pendingEvents.size());

    pendingEvents.forEach(eventProcessor::process);
  }
}
