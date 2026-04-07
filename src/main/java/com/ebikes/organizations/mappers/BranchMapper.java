package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.dtos.responses.branches.BranchAddressResponse;
import com.ebikes.organizations.dtos.responses.branches.BranchReference;
import com.ebikes.organizations.dtos.responses.branches.BranchResponse;
import com.ebikes.organizations.dtos.responses.branches.BranchSummaryResponse;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
    uses = {BranchAddressMapper.class})
public interface BranchMapper {

  @Mapping(target = "id", source = "branch.id")
  @Mapping(target = "logoUrl", source = "logoUrl")
  BranchReference toReference(Branch branch, String logoUrl);

  @Mapping(target = "address", source = "branchAddress")
  @Mapping(target = "createdAt", source = "branch.createdAt")
  @Mapping(target = "id", source = "branch.id")
  @Mapping(target = "organizationId", source = "branch.organization.id")
  @Mapping(target = "updatedAt", source = "branch.updatedAt")
  BranchResponse toResponse(Branch branch, BranchAddressResponse branchAddress);

  @Mapping(target = "createdAt", source = "createdAt")
  @Mapping(target = "id", source = "id")
  @Mapping(target = "organizationId", source = "organization.id")
  BranchSummaryResponse toSummaryResponse(Branch branch);
}
