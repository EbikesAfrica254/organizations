package com.ebikes.organizations.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.ebikes.organizations.database.entities.BranchAddress;
import com.ebikes.organizations.dtos.responses.branches.BranchAddressResponse;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface BranchAddressMapper {

  BranchAddressResponse toResponse(BranchAddress branchAddress);
}
