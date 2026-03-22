package com.ebikes.organizations.controllers;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.organizations.dtos.requests.filters.OutboxFilter;
import com.ebikes.organizations.dtos.responses.api.PaginatedResponse;
import com.ebikes.organizations.dtos.responses.api.SuccessResponse;
import com.ebikes.organizations.dtos.responses.outbox.OutboxResponse;
import com.ebikes.organizations.services.OutboxService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/outbox")
@RestController
public class OutboxController {

  private final OutboxService outboxService;

  @GetMapping
  public ResponseEntity<PaginatedResponse<OutboxResponse>> search(
      @Valid @ModelAttribute OutboxFilter filter) {
    return ResponseEntity.ok(outboxService.search(filter));
  }

  @PatchMapping("/{id}/retry")
  public ResponseEntity<SuccessResponse<Void>> retry(@PathVariable UUID id) {
    outboxService.retry(id);
    return ResponseEntity.ok(SuccessResponse.of(null, "Event reset to pending for retry"));
  }

  @PostMapping("/failed/retry")
  public ResponseEntity<SuccessResponse<Integer>> retryAll() {
    int count = outboxService.retryAllFailed();
    return ResponseEntity.ok(
        SuccessResponse.of(count, count + " failed event(s) reset to pending"));
  }
}
