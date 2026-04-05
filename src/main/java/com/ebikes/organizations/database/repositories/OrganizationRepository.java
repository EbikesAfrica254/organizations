package com.ebikes.organizations.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationReference;

@Repository
public interface OrganizationRepository
    extends JpaRepository<Organization, UUID>, JpaSpecificationExecutor<Organization> {

  boolean existsByLegalName(String legalName);

  List<OrganizationReference> findByIdIn(List<UUID> organizationIds);
}
