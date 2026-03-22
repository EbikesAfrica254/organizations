package com.ebikes.organizations.services;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.EventConstants;
import com.ebikes.organizations.constants.EventConstants.RoutingKeys;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.OrganizationRepository;
import com.ebikes.organizations.database.specifications.OrganizationSpecifications;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DocumentUploadInfo;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.enums.CheckerOutcome;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.publishers.AuditEventPublisher;
import com.ebikes.organizations.publishers.MakerCheckerPublisher;
import com.ebikes.organizations.support.audit.AuditMetadataBuilder;
import com.ebikes.organizations.support.changes.ChangeApplier;
import com.ebikes.organizations.support.changes.ChangeDetector;
import com.ebikes.organizations.support.changes.MakerCheckerRequestBuilder;
import com.ebikes.organizations.support.changes.SnapshotCreator;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.database.FilterUtilities;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrganizationService {

  private static final String ENTITY_TYPE = "ORGANIZATION";
  private static final String OPERATION_CREATE = "CREATE";
  private static final String OPERATION_UPDATE = "UPDATE";

  private final AuditEventPublisher auditEventPublisher;
  private final BranchService branchService;
  private final ChangeApplier changeApplier;
  private final ChangeDetector changeDetector;
  private final DocumentService documentService;
  private final MakerCheckerPublisher makerCheckerPublisher;
  private final ObjectMapper mapper;
  private final OrganizationRepository repository;
  private final SnapshotCreator snapshotCreator;

  @Transactional
  public Organization create(CreateOrganizationRequest request) {
    log.info("Creating organization: legalName={}", request.legalName());

    if (repository.existsByLegalName(request.legalName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Organization with legal name '" + request.legalName() + "' already exists");
    }

    Organization organization =
        new Organization(
            request.addresses(),
            request.displayName(),
            request.email(),
            request.incorporationDate(),
            request.kraPin(),
            request.legalName(),
            request.ownerId(),
            request.phoneNumber(),
            request.registrationNumber(),
            request.registrationType());
    log.info("Organization created: organizationId={}", mapper.writeValueAsString(organization));
    organization = repository.save(organization);

    documentService.associateWithOrganization(
        request.documents().stream().map(DocumentUploadInfo::key).toList(), organization);
    organization = requireById(organization.getId());

    List<FieldChange> changes = snapshotCreator.extractFields(organization);

    MakerCheckerRequest makerCheckerRequest =
        MakerCheckerRequestBuilder.forOrganizationCreate(
            organization, changes, ExecutionContext.getUserId());
    makerCheckerPublisher.publish(
        makerCheckerRequest, RoutingKeys.ORGANIZATIONS_ORGANIZATION_MAKER_CHECKER_REQUEST);

    log.info(
        "Organization created: organizationId={}, legalName={}",
        organization.getId(),
        organization.getLegalName());

    return organization;
  }

  @Transactional
  public Organization deactivate(UUID organizationId, String reason) {
    log.info("Deactivating organization: organizationId={}", organizationId);

    Organization organization = requireById(organizationId);
    organization.deactivate(reason);
    Organization saved = repository.save(organization);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Organizations.DEACTIVATED,
        AuditMetadataBuilder.forOrganization(saved, Map.of("deactivationReason", reason)),
        saved.getId().toString(),
        RoutingKeys.ORGANIZATIONS_ORGANIZATION_AUDIT,
        ExecutionContext.getUserId());

    log.info("Organization deactivated: organizationId={}", organizationId);

    return saved;
  }

  @Transactional(readOnly = true)
  public Organization findById(UUID organizationId) {
    return requireById(organizationId);
  }

  @Transactional
  public void handleApprovalDecision(MakerCheckerDecision decision) {
    Organization organization = requireById(decision.entityId());

    boolean approved = decision.outcome() == CheckerOutcome.APPROVED;

    switch (decision.operation()) {
      case OPERATION_CREATE -> {
        if (approved) {
          recordCreateApproval(organization);
        } else {
          recordRejection(organization, decision.operation(), decision.reason());
        }
      }
      case OPERATION_UPDATE -> {
        if (approved) {
          recordUpdateApproval(organization, decision.originalChanges());
        } else {
          recordRejection(organization, decision.operation(), decision.reason());
        }
      }
      default ->
          throw new IllegalArgumentException(
              "Unrecognised operation '"
                  + decision.operation()
                  + "' for entityId="
                  + decision.entityId());
    }
  }

  @Transactional(readOnly = true)
  public Page<Organization> search(OrganizationFilter filter) {
    Specification<Organization> spec = OrganizationSpecifications.buildSpecification(filter);
    Pageable pageable =
        FilterUtilities.buildPageable(filter, OrganizationSpecifications.ALLOWED_SORT_FIELDS);
    return repository.findAll(spec, pageable);
  }

  @Transactional
  public Organization update(UUID organizationId, UpdateOrganizationRequest request) {
    log.info("Updating organization: organizationId={}", organizationId);

    Organization existing = requireById(organizationId);

    if (request.legalName() != null
        && !request.legalName().equals(existing.getLegalName())
        && repository.existsByLegalName(request.legalName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Organization with legal name '" + request.legalName() + "' already exists");
    }

    List<FieldChange> changes = changeDetector.detectChanges(existing, request);

    if (changes.isEmpty()) {
      log.info("No changes detected for organization: organizationId={}", organizationId);
      return existing;
    }

    existing.resubmit();
    repository.save(existing);

    MakerCheckerRequest makerCheckerRequest =
        MakerCheckerRequestBuilder.forOrganizationUpdate(
            organizationId, changes, ExecutionContext.getUserId());
    makerCheckerPublisher.publish(
        makerCheckerRequest, RoutingKeys.ORGANIZATIONS_ORGANIZATION_MAKER_CHECKER_REQUEST);

    log.info(
        "Organization update submitted for approval: organizationId={}, changesCount={}",
        organizationId,
        changes.size());

    return existing;
  }

  Organization requireById(UUID organizationId) {
    return repository
        .findById(organizationId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND,
                    "Organization not found with ID: " + organizationId));
  }

  private void recordCreateApproval(Organization organization) {
    documentService.validateRequiredDocumentsUploaded(
        organization.getId(), organization.getRegistrationType());
    documentService.activateDocuments(organization.getId());

    organization.approve();
    organization.activate();
    Organization saved = repository.save(organization);
    branchService.createDefaultBranch(saved);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Organizations.APPROVED,
        AuditMetadataBuilder.forOrganization(saved),
        saved.getId().toString(),
        RoutingKeys.ORGANIZATIONS_ORGANIZATION_AUDIT,
        ExecutionContext.getUserId());

    log.info(
        "Organization creation approved and activated: organizationId={}", organization.getId());
  }

  private void recordRejection(Organization organization, String operation, String reason) {
    organization.reject(reason);
    Organization saved = repository.save(organization);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Organizations.REJECTED,
        AuditMetadataBuilder.forOrganization(
            saved, Map.of("operation", operation, "rejectionReason", reason)),
        saved.getId().toString(),
        RoutingKeys.ORGANIZATIONS_ORGANIZATION_AUDIT,
        ExecutionContext.getUserId());

    log.info("Organization {} rejected: organizationId={}", operation, saved.getId());
  }

  private void recordUpdateApproval(Organization organization, List<FieldChange> changes) {
    changeApplier.applyChanges(organization, changes);
    Organization saved = repository.save(organization);

    auditEventPublisher.publishSuccess(
        saved.getId(),
        ENTITY_TYPE,
        EventConstants.EventTypes.Organizations.APPROVED,
        AuditMetadataBuilder.forOrganization(
            saved, Map.of("changesCount", String.valueOf(changes.size()))),
        saved.getId().toString(),
        RoutingKeys.ORGANIZATIONS_ORGANIZATION_AUDIT,
        ExecutionContext.getUserId());

    log.info(
        "Organization update approved and applied: organizationId={}, changesCount={}",
        organization.getId(),
        changes.size());
  }
}
