package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.events.outgoing.NotificationRequest;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

  @Mapping(target = "branchId", ignore = true)
  @Mapping(target = "category", constant = "OPERATIONAL")
  @Mapping(target = "channel", constant = "EMAIL")
  @Mapping(target = "eventType", constant = "ORGANIZATION_WELCOME")
  @Mapping(target = "organizationId", expression = "java(organization.getId().toString())")
  @Mapping(target = "recipient", source = "organization.email")
  @Mapping(target = "serviceReference", source = "serviceReference")
  @Mapping(target = "subjectUserId", source = "organization.ownerId")
  @Mapping(target = "templateName", constant = "ORGANIZATION_WELCOME")
  @Mapping(target = "timestamp", ignore = true)
  @Mapping(target = "variables", ignore = true)
  NotificationRequest toOrganizationWelcome(Organization organization, String serviceReference);
}
