package com.ebikes.organizations.dtos.events.outgoing;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.organizations.constants.EventConstants.EventSource;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MakerCheckerRequest(
    String branchId,
    List<FieldChange> changes,
    @NotNull UUID entityId,
    @NotBlank String entityType,
    String makerId,
    Map<String, Object> operationContext,
    @NotBlank String organizationId,
    Instant requestedAt,
    String serviceReference)
    implements Serializable {

  public MakerCheckerRequest {
    changes = changes == null ? null : List.copyOf(changes);
    operationContext = operationContext == null ? null : Map.copyOf(operationContext);
    serviceReference = serviceReference == null ? EventSource.serviceReference() : serviceReference;
  }
}
