package com.ebikes.organizations.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.constants.EventConstants;
import com.ebikes.organizations.constants.EventConstants.RoutingKeys;
import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.BranchRepository;
import com.ebikes.organizations.database.specifications.AuthorizationSpecifications;
import com.ebikes.organizations.dtos.requests.branches.CreateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.UpdateBranchRequest;
import com.ebikes.organizations.enums.BranchStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.publishers.AuditEventPublisher;
import com.ebikes.organizations.support.audit.AuditMetadataBuilder;
import com.ebikes.organizations.support.context.ExecutionContext;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class BranchService {

  private static final String ENTITY_TYPE = "BRANCH";

  private final AuditEventPublisher auditEventPublisher;
  private final BranchAddressService branchAddressService;
  private final BranchRepository branchRepository;

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
            .build();

    Branch saved = branchRepository.save(branch);

    branchAddressService.create(saved, request.address());

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.CREATED,
        AuditMetadataBuilder.forBranch(saved),
        organization.getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ExecutionContext.getUserId());

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
            .displayName(organization.getDisplayName() + " - Main Branch")
            .email(organization.getEmail())
            .operatingHours(List.of())
            .organization(organization)
            .phoneNumber(organization.getPhoneNumber())
            .build();

    Branch saved = branchRepository.save(branch);

    branchAddressService.createFromOrganizationAddress(
        saved, organization.getAddresses().getFirst());

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.CREATED,
        AuditMetadataBuilder.forBranch(saved),
        organization.getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ApplicationConstants.SYSTEM_ID);

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
    Branch saved = branchRepository.save(branch);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.DEACTIVATED,
        AuditMetadataBuilder.forBranch(saved, Map.of("deactivationReason", reason)),
        saved.getOrganization().getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ExecutionContext.getUserId());

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
  public List<Branch> findByOrganization(UUID organizationId, boolean includeInactive) {
    AuthorizationSpecifications.assertOrganizationAccess(organizationId);
    return includeInactive
        ? branchRepository.findByOrganizationId(organizationId)
        : branchRepository.findByOrganizationIdAndStatusIn(
            organizationId, List.of(BranchStatus.ACTIVE, BranchStatus.SUSPENDED));
  }

  @Transactional
  public Branch reinstate(UUID organizationId, UUID branchId) {
    log.info("Reinstating branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    branch.reinstate();
    Branch saved = branchRepository.save(branch);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.REINSTATED,
        AuditMetadataBuilder.forBranch(saved),
        saved.getOrganization().getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ExecutionContext.getUserId());

    log.info("Branch reinstated: branchId={}", branchId);

    return saved;
  }

  @Transactional
  public Branch suspend(UUID organizationId, UUID branchId) {
    log.info("Suspending branch: branchId={}, organizationId={}", branchId, organizationId);

    Branch branch = requireById(branchId);
    AuthorizationSpecifications.assertBranchOwnership(branch, organizationId);
    branch.suspend();
    Branch saved = branchRepository.save(branch);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.SUSPENDED,
        AuditMetadataBuilder.forBranch(saved),
        saved.getOrganization().getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ExecutionContext.getUserId());

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
        && branchRepository.existsByOrganizationIdAndBranchNameIgnoreCase(
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

    Branch saved = branchRepository.save(branch);

    if (request.address() != null) {
      branchAddressService.update(branchId, request.address());
    }

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Branches.UPDATED,
        AuditMetadataBuilder.forBranch(saved),
        saved.getOrganization().getId().toString(),
        RoutingKeys.ORGANIZATIONS_BRANCH_AUDIT,
        ExecutionContext.getUserId());

    log.info("Branch updated: branchId={}", branchId);

    return saved;
  }

  Branch requireById(UUID branchId) {
    return branchRepository
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

    if (branchRepository.existsByOrganizationIdAndBranchNameIgnoreCase(
        organization.getId(), request.branchName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Branch with name '" + request.branchName() + "' already exists in this organization");
    }
  }
}
