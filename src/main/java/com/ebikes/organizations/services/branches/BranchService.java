package com.ebikes.organizations.services.branches;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.EventConstants.DomainEvents;
import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.BranchRepository;
import com.ebikes.organizations.database.specifications.AuthorizationSpecifications;
import com.ebikes.organizations.dtos.requests.branches.CreateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.UpdateBranchRequest;
import com.ebikes.organizations.dtos.responses.branches.BranchReference;
import com.ebikes.organizations.enums.BranchStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.mappers.BranchMapper;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.audit.AuditTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class BranchService {

  private final AuditTemplate auditTemplate;
  private final BranchAddressService branchAddressService;
  private final BranchMapper branchMapper;
  private final BranchRepository repository;
  private final StorageService storageService;

  @Transactional
  public Branch create(Organization organization, CreateBranchRequest request) {
    log.info(
        "Creating branch: organizationId={}, branchName={}",
        organization.getId(),
        request.branchName());

    AuthorizationSpecifications.assertOrganizationAccess(organization.getId());
    validateBranchCreation(organization, request);

    Branch branch =
        Branch.builder()
            .branchName(request.branchName())
            .displayName(request.displayName())
            .email(request.email())
            .operatingHours(request.operatingHours())
            .organization(organization)
            .phoneNumber(request.phoneNumber())
            .status(BranchStatus.ACTIVE)
            .build();

    Branch saved =
        auditTemplate.execute(
            branch,
            organization.getId().toString(),
            DomainEvents.Branch.CREATED,
            () -> repository.save(branch));

    branchAddressService.create(saved, request.address());

    log.info("Branch created: branchId={}, branchName={}", saved.getId(), request.branchName());

    return saved;
  }

  @Transactional
  public void createDefaultBranch(Organization organization) {
    log.info("Creating default branch for organization: organizationId={}", organization.getId());

    if (organization.getAddresses().isEmpty()) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Cannot create default branch - organization has no addresses",
          "addresses",
          organization.getId());
    }

    Branch branch =
        Branch.builder()
            .branchName("MAIN")
            .createdBy(organization.getCreatedBy())
            .displayName(organization.getDisplayName() + " - Main Branch")
            .email(organization.getEmail())
            .operatingHours(List.of())
            .organization(organization)
            .phoneNumber(organization.getPhoneNumber())
            .status(BranchStatus.ACTIVE)
            .build();

    Branch saved =
        auditTemplate.execute(
            branch,
            organization.getId().toString(),
            DomainEvents.Branch.CREATED,
            () -> repository.save(branch));

    branchAddressService.createFromOrganizationAddress(
        saved, organization.getAddresses().getFirst());

    log.info(
        "Default branch created: branchId={}, organizationId={}",
        saved.getId(),
        organization.getId());
  }

  @Transactional
  public Branch deactivate(UUID organizationId, UUID branchId, String reason) {
    log.info("Deactivating branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    branch.deactivate(reason);

    Branch saved =
        auditTemplate.execute(
            branch,
            organizationId.toString(),
            DomainEvents.Branch.DEACTIVATED,
            () -> repository.save(branch));

    log.info("Branch deactivated: branchId={}", branchId);

    return saved;
  }

  @Transactional(readOnly = true)
  public Branch findById(UUID organizationId, UUID branchId) {
    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    return branch;
  }

  @Transactional(readOnly = true)
  public List<BranchReference> findReferencesByIds(List<UUID> branchIds, UUID organizationId) {
    return repository.findByIdInAndOrganizationId(branchIds, organizationId).stream()
        .map(
            branch ->
                branchMapper.toReference(
                    branch,
                    branch.getOrganization().getLogoKey() != null
                        ? storageService.generatePreviewUrl(
                            branch.getOrganization().getLogoKey(), Duration.ofHours(1))
                        : null))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Branch> findByOrganization(UUID organizationId, boolean includeInactive) {
    AuthorizationSpecifications.assertOrganizationAccess(organizationId);
    return includeInactive
        ? repository.findByOrganizationId(organizationId)
        : repository.findByOrganizationIdAndStatusIn(
            organizationId, List.of(BranchStatus.ACTIVE, BranchStatus.SUSPENDED));
  }

  @Transactional
  public Branch reinstate(UUID organizationId, UUID branchId) {
    log.info("Reinstating branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    branch.reinstate();

    Branch saved =
        auditTemplate.execute(
            branch,
            organizationId.toString(),
            DomainEvents.Branch.REINSTATED,
            () -> repository.save(branch));

    log.info("Branch reinstated: branchId={}", branchId);

    return saved;
  }

  @Transactional
  public Branch suspend(UUID organizationId, UUID branchId) {
    log.info("Suspending branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    branch.suspend();

    Branch saved =
        auditTemplate.execute(
            branch,
            organizationId.toString(),
            DomainEvents.Branch.SUSPENDED,
            () -> repository.save(branch));

    log.info("Branch suspended: branchId={}", branchId);

    return saved;
  }

  @Transactional
  public Branch update(UUID organizationId, UUID branchId, UpdateBranchRequest request) {
    log.info("Updating branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);

    if (request.branchName() != null
        && !request.branchName().equalsIgnoreCase(branch.getBranchName())
        && repository.existsByOrganizationIdAndBranchNameIgnoreCase(
            branch.getOrganization().getId(), request.branchName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Branch with name '" + request.branchName() + "' already exists in this organization");
    }

    branch.update(
        request.branchName(),
        request.displayName(),
        request.email(),
        request.phoneNumber(),
        request.operatingHours());

    Branch saved =
        auditTemplate.execute(
            branch,
            organizationId.toString(),
            DomainEvents.Branch.UPDATED,
            () -> repository.save(branch));

    if (request.address() != null) {
      branchAddressService.update(branchId, request.address());
    }

    log.info("Branch updated: branchId={}", branchId);

    return saved;
  }

  Branch requireById(UUID branchId) {
    return repository
        .findById(branchId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Branch not found with ID: " + branchId));
  }

  private void validateBranchCreation(Organization organization, CreateBranchRequest request) {
    if (organization.getStatus() != OrganizationStatus.ACTIVE) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Organization must be ACTIVE to create branches. Current status: "
              + organization.getStatus(),
          "organizationStatus",
          organization.getStatus());
    }

    if (repository.existsByOrganizationIdAndBranchNameIgnoreCase(
        organization.getId(), request.branchName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Branch with name '" + request.branchName() + "' already exists in this organization");
    }
  }
}
