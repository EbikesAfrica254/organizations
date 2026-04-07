package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationReference;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationResponse;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationSummaryResponse;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface OrganizationMapper {

  @Mapping(target = "id", source = "organization.id")
  @Mapping(target = "displayName", source = "organization.displayName")
  @Mapping(target = "address", source = "address")
  @Mapping(target = "logoUrl", source = "logoUrl")
  OrganizationReference toReference(Organization organization, String address, String logoUrl);

  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "id", source = "id")
  @Mapping(target = "updatedAt", source = "updatedAt")
  OrganizationResponse toResponse(Organization organization);

  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "id", source = "id")
  OrganizationSummaryResponse toSummaryResponse(Organization organization);
}
