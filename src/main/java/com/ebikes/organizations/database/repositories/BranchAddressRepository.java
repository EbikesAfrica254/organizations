package com.ebikes.organizations.database.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ebikes.organizations.database.entities.BranchAddress;
import com.ebikes.organizations.enums.AddressTag;

public interface BranchAddressRepository extends JpaRepository<BranchAddress, UUID> {

  Optional<BranchAddress> findByBranchIdAndAddressTag(UUID branchId, AddressTag addressTag);
}
