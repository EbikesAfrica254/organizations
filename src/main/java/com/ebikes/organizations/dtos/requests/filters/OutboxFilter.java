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
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private String eventType;
  private Integer maxRetryCount;
  private Integer minRetryCount;
  private OutboxStatus status;
  private OffsetDateTime updatedAtFrom;
  private OffsetDateTime updatedAtTo;
}
