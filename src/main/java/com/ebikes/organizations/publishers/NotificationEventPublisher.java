package com.ebikes.organizations.publishers;

import org.springframework.stereotype.Component;

import com.ebikes.organizations.configurations.properties.NotificationProperties;
import com.ebikes.organizations.constants.EventConstants.RoutingKeys;
import com.ebikes.organizations.dtos.events.outgoing.NotificationRequest;
import com.ebikes.organizations.enums.ChannelType;
import com.ebikes.organizations.services.events.OutboxService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventPublisher {

  private final OutboxService outboxService;
  private final NotificationProperties notificationProperties;

  public void publish(NotificationRequest request) {
    String routingKey = resolveRoutingKey(request.channel());
    if (notificationProperties.isEnabled()) {
      outboxService.publish(request.eventType(), request, routingKey);
      log.info(
          "Notification event queued - eventType={} channel={} recipient={}",
          request.eventType(),
          request.channel(),
          request.recipient());
    } else {
      log.info("Notification service is disabled");
    }
  }

  private String resolveRoutingKey(ChannelType channel) {
    return switch (channel) {
      case EMAIL -> RoutingKeys.NOTIFICATIONS_EMAIL;
      case SMS -> RoutingKeys.NOTIFICATIONS_SMS;
      case SSE -> RoutingKeys.NOTIFICATIONS_SSE;
      case WHATSAPP -> RoutingKeys.NOTIFICATIONS_WHATSAPP;
    };
  }
}
