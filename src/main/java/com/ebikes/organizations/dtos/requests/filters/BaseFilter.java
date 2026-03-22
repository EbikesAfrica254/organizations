package com.ebikes.organizations.dtos.requests.filters;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BaseFilter {
  @Min(1) private Integer page = 1;

  @Min(1) @Max(100) private Integer size = 20;

  private String sortBy = "createdAt";

  private String sortDirection = "DESC";
}
