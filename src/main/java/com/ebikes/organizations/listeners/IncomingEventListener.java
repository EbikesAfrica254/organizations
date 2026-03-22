package com.ebikes.organizations.listeners;

import java.io.IOException;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.support.context.EventContext;
import com.ebikes.organizations.support.events.EventContextDecorator;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class IncomingEventListener {

  private final List<IncomingEventHandler> handlers;

  public IncomingEventListener(List<IncomingEventHandler> handlers) {
    this.handlers = List.copyOf(handlers);
  }

  public void route(Message<?> message) {
    EventContextDecorator.decorate(message, () -> handle(message));
  }

  private void handle(Message<?> message) {
    String routingKey = EventContext.getRoutingKey();

    if (routingKey == null || routingKey.isBlank()) {
      log.warn("Received message with no routingKey header, discarding");
      return;
    }

    IncomingEventHandler handler =
        handlers.stream().filter(h -> h.matches(routingKey)).findFirst().orElse(null);

    if (handler == null) {
      log.warn("No handler registered for routingKey={}, discarding", routingKey);
      return;
    }

    try {
      handler.handle((byte[]) message.getPayload());
    } catch (IOException e) {
      log.error("Failed to deserialize message payload for routingKey={}", routingKey, e);
    } catch (Exception e) {
      log.error("Failed to process message for routingKey={}", routingKey, e);
    }
  }
}
