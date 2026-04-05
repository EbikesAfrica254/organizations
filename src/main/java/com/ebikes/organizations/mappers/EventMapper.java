package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.events.outgoing.OrganizationCreatedEvent;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface EventMapper {
  @Mapping(target = "displayName", source = "organization.displayName")
  @Mapping(target = "organizationId", source = "organization.id")
  @Mapping(target = "serviceReference", source = "serviceReference")
  OrganizationCreatedEvent toOrganizationCreatedEvent(
      Organization organization, String serviceReference);
}
