package com.ebikes.organizations.dtos.responses.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PaginatedResponse<T>(
    List<T> data,
    boolean first,
    boolean last,
    String message,
    int page,
    int size,
    long totalElements,
    int totalPages) {

  public static <T> PaginatedResponse<T> from(String message, Page<T> page) {
    return new PaginatedResponse<>(
        page.getContent(),
        page.isFirst(),
        page.isLast(),
        message,
        page.getNumber(),
        page.getSize(),
        page.getTotalElements(),
        page.getTotalPages());
  }
}
