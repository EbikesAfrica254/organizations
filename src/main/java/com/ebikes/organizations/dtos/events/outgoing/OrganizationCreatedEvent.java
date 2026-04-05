package com.ebikes.organizations.dtos.events.outgoing;

import java.io.Serializable;

public record OrganizationCreatedEvent(
    String displayName, String organizationId, String ownerId, String serviceReference)
    implements Serializable {}
