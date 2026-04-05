package com.ebikes.organizations.support.fixtures;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.CheckerOutcome;

public final class MakerCheckerFixtures {

  private MakerCheckerFixtures() {}

  public static MakerCheckerDecision approved(
      UUID entityId, String entityType, String operation, List<FieldChange> changes) {
    return new MakerCheckerDecision(
        null,
        Instant.now(),
        entityId,
        entityType,
        operation,
        changes,
        CheckerOutcome.APPROVED,
        null,
        UUID.randomUUID().toString());
  }

  public static MakerCheckerDecision approved(UUID entityId, String entityType, String operation) {
    return approved(entityId, entityType, operation, List.of());
  }

  public static MakerCheckerDecision rejected(
      UUID entityId, String entityType, String operation, String reason) {
    return new MakerCheckerDecision(
        null,
        Instant.now(),
        entityId,
        entityType,
        operation,
        List.of(),
        CheckerOutcome.REJECTED,
        reason,
        UUID.randomUUID().toString());
  }
}
