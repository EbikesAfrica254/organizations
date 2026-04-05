package com.ebikes.organizations.dtos.responses.outbox;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.ebikes.organizations.enums.OutboxStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OutboxResponse(
    UUID id,
    OffsetDateTime createdAt,
    String eventType,
    Integer retryCount,
    String routingKey,
    OutboxStatus status,
    OffsetDateTime updatedAt)
    implements Serializable {}
