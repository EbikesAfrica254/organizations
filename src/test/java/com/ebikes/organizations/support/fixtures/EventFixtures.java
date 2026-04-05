package com.ebikes.organizations.support.fixtures;

import com.ebikes.organizations.dtos.events.outgoing.OrganizationCreatedEvent;

public final class EventFixtures {

  private EventFixtures() {}

  public static OrganizationCreatedEvent organizationCreatedEvent() {
    return new OrganizationCreatedEvent(
        "Test Organization",
        SecurityFixtures.TEST_ORGANIZATION_ID,
        SecurityFixtures.TEST_USER_ID,
        "test-service-reference");
  }
}
