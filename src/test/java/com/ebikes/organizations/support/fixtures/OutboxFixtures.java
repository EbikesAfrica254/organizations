package com.ebikes.organizations.support.fixtures;

import java.util.UUID;

import com.ebikes.organizations.database.entities.Outbox;

public final class OutboxFixtures {

  private OutboxFixtures() {}

  public static Outbox pending(String eventType) {
    return base(eventType).build();
  }

  public static Outbox failed(String eventType) {
    Outbox outbox = base(eventType).build();
    outbox.markFailed();
    return outbox;
  }

  public static Outbox failedWithRetries(String eventType, int retries) {
    Outbox outbox = base(eventType).build();
    for (int i = 0; i < retries; i++) {
      outbox.markFailed();
      outbox.resetForRetry();
    }
    outbox.markFailed();
    return outbox;
  }

  private static Outbox.OutboxBuilder<?, ?> base(String eventType) {
    return Outbox.builder()
        .id(UUID.randomUUID())
        .eventType(eventType)
        .payload(EventFixtures.organizationCreatedEvent())
        .routingKey("test.routing.key");
  }
}
