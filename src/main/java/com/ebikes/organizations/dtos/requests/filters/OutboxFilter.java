package com.ebikes.organizations.dtos.requests.filters;

import java.time.OffsetDateTime;

import com.ebikes.organizations.enums.OutboxStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OutboxFilter extends BaseFilter {

  private OffsetDateTime createdAtAfter;
  private OffsetDateTime createdAtBefore;
  private String eventType;
  private Integer maxRetryCount;
  private Integer minRetryCount;
  private String routingKey;
  private OutboxStatus status;
  private OffsetDateTime updatedAtAfter;
  private OffsetDateTime updatedAtBefore;
}
