package com.ebikes.organizations.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.organizations.dtos.responses.api.PaginatedResponse;
import com.ebikes.organizations.services.events.OutboxService;
import com.ebikes.organizations.support.infrastructure.AbstractControllerTest;

@DisplayName("OutboxController")
@WebMvcTest(OutboxController.class)
class OutboxControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private OutboxService outboxService;

  @Nested
  @DisplayName("GET /outbox")
  class Search {

    @Test
    @DisplayName("should return 200 with paginated results")
    void shouldReturn200WithPaginatedResults() throws Exception {
      when(outboxService.search(any()))
          .thenReturn(PaginatedResponse.from("Outbox events retrieved", Page.empty()));

      mockMvc.perform(get("/outbox").with(authenticatedJwt())).andExpect(status().isOk());

      verify(outboxService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/outbox").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /outbox/{id}/retry")
  class Retry {

    @Test
    @DisplayName("should return 200 when outbox event exists")
    void shouldReturn200WhenOutboxEventExists() throws Exception {
      mockMvc
          .perform(patch("/outbox/{id}/retry", UUID.randomUUID()).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(outboxService).retry(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(patch("/outbox/{id}/retry", UUID.randomUUID()).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /outbox/failed/retry")
  class RetryAll {

    @Test
    @DisplayName("should return 200 with retry count")
    void shouldReturn200WithRetryCount() throws Exception {
      when(outboxService.retryAllFailed()).thenReturn(3);

      mockMvc
          .perform(post("/outbox/failed/retry").with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(outboxService).retryAllFailed();
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(post("/outbox/failed/retry").with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }
}
