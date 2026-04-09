package com.ebikes.organizations.services.notifications;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.events.outgoing.NotificationRequest;
import com.ebikes.organizations.enums.ChannelType;
import com.ebikes.organizations.mappers.NotificationMapper;
import com.ebikes.organizations.publishers.NotificationEventPublisher;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("NotificationService")
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString(SecurityFixtures.TEST_ORGANIZATION_ID);

  @Mock private NotificationEventPublisher notificationEventPublisher;
  @Mock private NotificationMapper notificationMapper;

  private NotificationService service;
  private Organization organization;

  @BeforeEach
  void setUp() {
    service = new NotificationService(notificationEventPublisher, notificationMapper);
    organization = OrganizationFixtures.active();
    ReflectionTestUtils.setField(organization, "id", ORGANIZATION_ID);
  }

  @Test
  @DisplayName("should map organization to NotificationRequest and delegate to publisher")
  void shouldMapAndPublish() {
    NotificationRequest request =
        new NotificationRequest(
            null,
            null,
            ChannelType.EMAIL,
            "ORGANIZATION_WELCOME",
            ORGANIZATION_ID.toString(),
            organization.getEmail(),
            "any-reference",
            organization.getOwnerId(),
            "ORGANIZATION_WELCOME",
            null,
            null);

    when(notificationMapper.toOrganizationWelcome(eq(organization), any(String.class)))
        .thenReturn(request);

    service.sendOrganizationWelcome(organization);

    verify(notificationMapper).toOrganizationWelcome(eq(organization), any(String.class));
    verify(notificationEventPublisher).publish(request);
  }
}
