package com.ebikes.organizations.dtos.events.outgoing;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.organizations.constants.EventConstants.EventSource;
import com.ebikes.organizations.enums.AuditOutcome;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEvent(
    UUID entityId,
    @NotBlank String entityType,
    String eventType,
    String failureReason,
    String ipAddress,
    Map<String, String> metadata,
    String organizationId,
    @NotNull AuditOutcome outcome,
    String serviceReference,
    Instant timestamp,
    String userId)
    implements Serializable {

  public AuditEvent {
    metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    serviceReference = serviceReference == null ? EventSource.serviceReference() : serviceReference;
    timestamp = timestamp == null ? Instant.now() : timestamp;
  }
}
