package com.ebikes.organizations.services;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.BranchAddress;
import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.database.repositories.BranchAddressRepository;
import com.ebikes.organizations.dtos.requests.branches.BranchAddressRequest;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class BranchAddressService {

  private final BranchAddressRepository branchAddressRepository;

  @Transactional
  public void create(Branch branch, BranchAddressRequest request) {
    log.info("Creating branch address: branchId={}", branch.getId());

    BranchAddress branchAddress =
        BranchAddress.builder()
            .addressTag(AddressTag.BRANCH_LOCATION)
            .branch(branch)
            .city(request.city())
            .country(request.country())
            .latitude(request.latitude())
            .longitude(request.longitude())
            .postalCode(request.postalCode())
            .streetAddress(request.streetAddress())
            .build();

    BranchAddress saved = branchAddressRepository.save(branchAddress);

    log.info("Branch address created: branchAddressId={}", saved.getId());
  }

  @Transactional
  public void createFromOrganizationAddress(Branch branch, Address organizationAddress) {
    log.info("Creating branch address from organization address: branchId={}", branch.getId());

    BranchAddress branchAddress =
        BranchAddress.builder()
            .addressTag(AddressTag.BRANCH_LOCATION)
            .branch(branch)
            .city(organizationAddress.city())
            .country(organizationAddress.country())
            .latitude(organizationAddress.latitude())
            .longitude(organizationAddress.longitude())
            .postalCode(organizationAddress.postalCode())
            .streetAddress(organizationAddress.streetAddress())
            .build();

    BranchAddress saved = branchAddressRepository.save(branchAddress);

    log.info("Branch address created from organization address: branchAddressId={}", saved.getId());
  }

  @Transactional(readOnly = true)
  public BranchAddress findBranchLocationByBranchId(UUID branchId) {
    return requireByBranchId(branchId);
  }

  @Transactional
  public void update(UUID branchId, BranchAddressRequest request) {
    log.info("Updating branch address: branchId={}", branchId);

    BranchAddress branchAddress = this.requireByBranchId(branchId);
    branchAddress.update(request);
    BranchAddress saved = branchAddressRepository.save(branchAddress);

    log.info("Branch address updated: branchAddressId={}", saved.getId());
  }

  private BranchAddress requireByBranchId(UUID branchId){
    return branchAddressRepository
            .findByBranchIdAndAddressTag(branchId, AddressTag.BRANCH_LOCATION)
            .orElseThrow(
                    () ->
                            new ResourceNotFoundException(
                                    ResponseCode.RESOURCE_NOT_FOUND,
                                    "Branch address not found for branch: " + branchId));
  }
}
