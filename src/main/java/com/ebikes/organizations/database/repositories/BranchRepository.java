package com.ebikes.organizations.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.enums.BranchStatus;

public interface BranchRepository
    extends JpaRepository<Branch, UUID>, JpaSpecificationExecutor<Branch> {

  boolean existsByOrganizationIdAndBranchNameIgnoreCase(UUID organizationId, String branchName);

  List<Branch> findByOrganizationId(UUID organizationId);

  List<Branch> findByOrganizationIdAndStatusIn(UUID organizationId, List<BranchStatus> statuses);
}
