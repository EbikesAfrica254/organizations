package com.ebikes.organizations.listeners;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.ebikes.organizations.constants.ApplicationConstants.MessageHeaders;

@DisplayName("IncomingEventListener")
@ExtendWith(MockitoExtension.class)
class IncomingEventListenerTest {

  private static final String ROUTING_KEY = "organizations.organization.created";
  private static final byte[] PAYLOAD = "{}".getBytes();

  @Mock private IncomingEventHandler handler;

  private IncomingEventListener listener;

  @BeforeEach
  void setUp() {
    listener = new IncomingEventListener(List.of(handler));
  }

  @Test
  @DisplayName("should discard message when routingKey header is missing")
  void shouldDiscardWhenRoutingKeyMissing() throws IOException {
    Message<byte[]> message = MessageBuilder.withPayload(PAYLOAD).build();

    listener.route(message);

    verify(handler, never()).handle(any());
  }

  @Test
  @DisplayName("should discard message when routingKey header is blank")
  void shouldDiscardWhenRoutingKeyBlank() throws IOException {
    Message<byte[]> message =
        MessageBuilder.withPayload(PAYLOAD).setHeader(MessageHeaders.ROUTING_KEY, "").build();

    listener.route(message);

    verify(handler, never()).handle(any());
  }

  @Test
  @DisplayName("should discard message when no handler matches routingKey")
  void shouldDiscardWhenNoHandlerMatches() throws IOException {
    when(handler.matches(ROUTING_KEY)).thenReturn(false);

    Message<byte[]> message =
        MessageBuilder.withPayload(PAYLOAD)
            .setHeader(MessageHeaders.ROUTING_KEY, ROUTING_KEY)
            .build();

    listener.route(message);

    verify(handler, never()).handle(any());
  }

  @Test
  @DisplayName("should invoke matching handler with message payload")
  void shouldInvokeMatchingHandler() throws IOException {
    when(handler.matches(ROUTING_KEY)).thenReturn(true);

    Message<byte[]> message =
        MessageBuilder.withPayload(PAYLOAD)
            .setHeader(MessageHeaders.ROUTING_KEY, ROUTING_KEY)
            .build();

    listener.route(message);

    verify(handler).handle(PAYLOAD);
  }

  @Test
  @DisplayName("should swallow exception thrown by handler")
  void shouldSwallowHandlerException() throws IOException {
    when(handler.matches(ROUTING_KEY)).thenReturn(true);
    doThrow(new RuntimeException("handler error")).when(handler).handle(any());

    Message<byte[]> message =
        MessageBuilder.withPayload(PAYLOAD)
            .setHeader(MessageHeaders.ROUTING_KEY, ROUTING_KEY)
            .build();

    assertThatNoException().isThrownBy(() -> listener.route(message));
  }
}
