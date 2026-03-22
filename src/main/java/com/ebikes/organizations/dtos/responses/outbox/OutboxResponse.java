package com.ebikes.organizations.dtos.responses.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.OutboxStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutboxResponse(
    UUID id,
    OutboxStatus status,
    String eventType,
    Integer retryCount,
    String routingKey,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
