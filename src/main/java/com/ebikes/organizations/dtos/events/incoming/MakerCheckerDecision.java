package com.ebikes.organizations.dtos.events.incoming;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.CheckerOutcome;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MakerCheckerDecision(
    String checkerId,
    @NotNull Instant decidedAt,
    @NotNull UUID entityId,
    @NotBlank String entityType,
    String operation,
    List<FieldChange> originalChanges,
    @NotNull CheckerOutcome outcome,
    String reason,
    @NotBlank String serviceReference)
    implements Serializable {

  public MakerCheckerDecision {
    originalChanges = originalChanges == null ? List.of() : List.copyOf(originalChanges);
  }
}
