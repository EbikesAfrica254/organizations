package com.ebikes.organizations.services.events;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.database.specifications.OutboxSpecifications;
import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.dtos.responses.api.PaginatedResponse;
import com.ebikes.organizations.dtos.responses.outbox.OutboxResponse;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.mappers.OutboxMapper;
import com.ebikes.organizations.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OutboxService {
  private final OutboxMapper mapper;
  private final OutboxRepository repository;

  @Transactional
  public void retry(UUID outboxId) {
    log.info("Retrying failed outbox event: outboxId={}", outboxId);

    Outbox outbox = requireById(outboxId);
    outbox.resetForRetry();
    repository.save(outbox);

    log.info("Outbox event reset to PENDING: outboxId={}", outboxId);
  }

  @Transactional
  public int retryAllFailed() {
    log.info("Retrying all failed outbox events");

    List<Outbox> failedEvents = repository.findByStatusOrderByIdAsc(OutboxStatus.FAILED);
    failedEvents.forEach(Outbox::resetForRetry);
    repository.saveAll(failedEvents);

    log.info("Reset {} failed outbox events to PENDING", failedEvents.size());

    return failedEvents.size();
  }

  @Transactional
  public void publish(String eventType, Object payload, String routingKey) {
    Outbox outbox =
        Outbox.builder().eventType(eventType).payload(payload).routingKey(routingKey).build();

    repository.save(outbox);

    log.debug("Outbox record created: eventType={}, outboxId={}", eventType, outbox.getId());
  }

  @Transactional(readOnly = true)
  public PaginatedResponse<OutboxResponse> search(OutboxFilter filter) {
    Specification<Outbox> spec = OutboxSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, OutboxSpecifications.ALLOWED_SORT_FIELDS);
    Page<OutboxResponse> page = repository.findAll(spec, pageable).map(mapper::toResponse);
    return PaginatedResponse.from("Outbox events retrieved", page);
  }

  public Outbox requireById(UUID outboxId) {
    return repository
        .findById(outboxId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Outbox event not found: " + outboxId));
  }
}
