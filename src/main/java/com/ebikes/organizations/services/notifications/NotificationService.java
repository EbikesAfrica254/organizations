package com.ebikes.organizations.services.notifications;

import org.springframework.stereotype.Service;

import com.ebikes.organizations.constants.EventConstants.Source;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.events.outgoing.NotificationRequest;
import com.ebikes.organizations.mappers.NotificationMapper;
import com.ebikes.organizations.publishers.NotificationEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class NotificationService {

  private final NotificationEventPublisher notificationEventPublisher;
  private final NotificationMapper notificationMapper;

  public void sendOrganizationWelcome(Organization organization) {
    NotificationRequest request =
        notificationMapper.toOrganizationWelcome(organization, Source.serviceReference());
    notificationEventPublisher.publish(request);
    log.info("Organization welcome notification queued: organizationId={}", organization.getId());
  }
}
