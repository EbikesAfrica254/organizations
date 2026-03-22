package com.ebikes.organizations.services;

import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.constants.EventConstants.MessageHeaders;
import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.database.repositories.OutboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessor {

  private final OutboxRepository repository;
  private final StreamBridge streamBridge;

  @Transactional
  public void process(Outbox outbox) {
    try {
      Message<?> message = buildMessage(outbox);
      boolean sent = streamBridge.send(ApplicationConstants.Outbox.BINDING_NAME, message);

      if (sent) {
        outbox.markSent();
        repository.save(outbox);
        log.debug(
            "Published outbox event: outboxId={}, eventType={}",
            outbox.getId(),
            outbox.getEventType());
      } else {
        handleFailure(outbox);
      }
    } catch (Exception e) {
      log.error(
          "Failed to publish outbox event: outboxId={}, eventType={}",
          outbox.getId(),
          outbox.getEventType(),
          e);
      handleFailure(outbox);
    }
  }

  private void handleFailure(Outbox outbox) {
    outbox.markFailed();

    if (outbox.getRetryCount() >= ApplicationConstants.Outbox.MAX_RETRY_COUNT) {
      outbox.markDeadLetter();
      log.error(
          "Outbox event moved to DEAD_LETTER after {} retries: outboxId={}, eventType={}",
          ApplicationConstants.Outbox.MAX_RETRY_COUNT,
          outbox.getId(),
          outbox.getEventType());
    }

    repository.save(outbox);
  }

  private Message<?> buildMessage(Outbox outbox) {
    return MessageBuilder.withPayload(outbox.getPayload())
        .setHeader(MessageHeaders.EVENT_TYPE, outbox.getEventType())
        .setHeader(MessageHeaders.OUTBOX_ID, outbox.getId().toString())
        .setHeader(MessageHeaders.ROUTING_KEY, outbox.getRoutingKey())
        .build();
  }
}
