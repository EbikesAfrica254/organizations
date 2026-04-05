package com.ebikes.organizations.support.events;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.ebikes.organizations.constants.ApplicationConstants.MessageHeaders;
import com.ebikes.organizations.constants.EventConstants.Source;
import com.ebikes.organizations.support.context.EventContext;
import com.ebikes.organizations.support.context.ExecutionContext;

@DisplayName("EventContextDecorator")
class EventContextDecoratorTest {

  private static final String OUTBOX_ID = "outbox-123";
  private static final String EVENT_TYPE = "organizations.organization.created";
  private static final String ROUTING_KEY = "organizations.organization.audit";

  @AfterEach
  void tearDown() {
    ExecutionContext.clear();
    EventContext.clear();
  }

  private Message<byte[]> message() {
    return MessageBuilder.withPayload("{}".getBytes())
        .setHeader(MessageHeaders.OUTBOX_ID, OUTBOX_ID)
        .setHeader(MessageHeaders.EVENT_TYPE, EVENT_TYPE)
        .setHeader(MessageHeaders.ROUTING_KEY, ROUTING_KEY)
        .build();
  }

  @Test
  @DisplayName("should set ExecutionContext as system before running handler")
  void shouldSetExecutionContextBeforeHandler() {
    EventContextDecorator.decorate(
        message(),
        () ->
            assertThat(ExecutionContext.get()).isInstanceOf(ExecutionContext.SystemContext.class));
  }

  @Test
  @DisplayName("should set EventContext with message headers before running handler")
  void shouldSetEventContextBeforeHandler() {
    EventContextDecorator.decorate(
        message(),
        () -> {
          assertThat(EventContext.getEventType()).isEqualTo(EVENT_TYPE);
          assertThat(EventContext.getRoutingKey()).isEqualTo(ROUTING_KEY);
          assertThat(EventContext.getSourceService()).isEqualTo(Source.ORGANIZATIONS);
        });
  }

  @Test
  @DisplayName("should clear ExecutionContext and EventContext after handler completes")
  void shouldClearContextsAfterHandler() {
    EventContextDecorator.decorate(message(), () -> {});

    assertThat(EventContext.absent()).isTrue();
    assertThatThrownBy(ExecutionContext::getUserId).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @DisplayName("should clear contexts even when handler throws")
  void shouldClearContextsWhenHandlerThrows() {
    try {
      EventContextDecorator.decorate(
          message(),
          () -> {
            throw new RuntimeException("handler error");
          });
    } catch (RuntimeException ignored) {
      // expected
    }

    assertThat(EventContext.absent()).isTrue();
    assertThatThrownBy(ExecutionContext::getUserId).isInstanceOf(IllegalStateException.class);
  }
}
