package com.ebikes.organizations.services.organizations;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.constants.EventConstants.DomainEvents;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.OrganizationRepository;
import com.ebikes.organizations.database.specifications.OrganizationSpecifications;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DocumentUploadInfo;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationReference;
import com.ebikes.organizations.enums.CheckerOutcome;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.mappers.OrganizationMapper;
import com.ebikes.organizations.services.branches.BranchService;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.audit.AuditTemplate;
import com.ebikes.organizations.support.changes.ChangeApplier;
import com.ebikes.organizations.support.changes.ChangeDetector;
import com.ebikes.organizations.support.changes.SnapshotCreator;
import com.ebikes.organizations.support.database.FilterUtilities;
import com.ebikes.organizations.support.makerchecker.MakerCheckerTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrganizationService {

  private static final String OPERATION_CREATE = "CREATE";
  private static final String OPERATION_UPDATE = "UPDATE";

  private final AuditTemplate auditTemplate;
  private final BranchService branchService;
  private final ChangeApplier changeApplier;
  private final ChangeDetector changeDetector;
  private final DocumentService documentService;
  private final MakerCheckerTemplate makerCheckerTemplate;
  private final OrganizationMapper organizationMapper;
  private final OrganizationRepository repository;
  private final SnapshotCreator snapshotCreator;
  private final StorageService storageService;

  @Transactional
  public Organization create(CreateOrganizationRequest request) {
    log.info("Creating organization: legalName={}", request.legalName());

    if (repository.existsByLegalName(request.legalName())) {
      throw new DuplicateResourceException(
          ResponseCode.DUPLICATE_RESOURCE,
          "Organization with legal name '" + request.legalName() + "' already exists");
    }

    Organization organization =
        Organization.builder()
            .addresses(request.addresses())
            .displayName(request.displayName())
            .email(request.email())
            .incorporationDate(request.incorporationDate())
            .kraPin(request.kraPin())
            .legalName(request.legalName())
            .ownerId(request.ownerId())
            .phoneNumber(request.phoneNumber())
            .registrationNumber(request.registrationNumber())
            .registrationType(request.registrationType())
            .build();
    organization = repository.save(organization);

    documentService.associateWithOrganization(
        request.documents().stream().map(DocumentUploadInfo::key).toList(), organization);

    organization = requireById(organization.getId());

    List<FieldChange> changes = snapshotCreator.extractFields(organization);

    makerCheckerTemplate.publish(
        organization, organization.getId().toString(), OPERATION_CREATE, changes);

    log.info(
        "Organization created: organizationId={}, legalName={}",
        organization.getId(),
        organization.getLegalName());

    return organization;
  }

  @Transactional
  public Organization deactivate(UUID organizationId, String reason) {
    Organization organization = requireById(organizationId);
    organization.deactivate(reason);
    Organization updated =
        auditTemplate.execute(
            organization,
            organization.getId().toString(),
            DomainEvents.Organization.DEACTIVATED,
            () -> repository.save(organization));

    log.info("Organization deactivated: organizationId={}", updated.getId());
    return updated;
  }

  @Transactional(readOnly = true)
  public Organization findById(UUID organizationId) {
    return requireById(organizationId);
  }

  @Transactional(readOnly = true)
  public List<OrganizationReference> findReferencesByIds(List<UUID> organizationIds) {
    return repository.findByIdIn(organizationIds).stream()
        .map(
            organization ->
                organizationMapper.toReference(
                    organization,
                    organization.getAddresses().isEmpty()
                        ? null
                        : organization.getAddresses().getFirst().toFormattedString(),
                    organization.getLogoKey() != null
                        ? storageService.generatePreviewUrl(
                            organization.getLogoKey(), Duration.ofHours(1))
                        : null))
        .toList();
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
  public String updateLogoKey(UUID organizationId, String newKey) {
    Organization organization = requireById(organizationId);
    String previousKey = organization.getLogoKey();
    organization.updateLogoKey(newKey);
    repository.save(organization);
    log.info("Logo key updated: organizationId={}", organizationId);
    return previousKey;
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

    List<FieldChange> changes = snapshotCreator.extractChanges(request, existing);

    if (changes.isEmpty()) {
      log.info("No changes detected for organization: organizationId={}", organizationId);
      return existing;
    }

    existing.resubmit();
    repository.save(existing);

    makerCheckerTemplate.publish(existing, existing.getId().toString(), OPERATION_UPDATE, changes);

    log.info(
        "Organization update submitted for approval: organizationId={}, changesCount={}",
        organizationId,
        changes.size());

    return existing;
  }

  @Transactional
  public void updateComplianceStatus(Organization organization) {
    boolean compliant =
        documentService.hasAllRequiredDocumentsActive(
            organization.getId(), organization.getRegistrationType());

    ComplianceStatus newStatus =
        compliant ? ComplianceStatus.COMPLIANT : ComplianceStatus.NON_COMPLIANT;

    if (organization.getComplianceStatus() == newStatus) {
      log.debug("Compliance status unchanged: organizationId={}", organization.getId());
      return;
    }

    auditTemplate.execute(
        organization,
        organization.getId().toString(),
        DomainEvents.Organization.COMPLIANCE_UPDATED,
        () -> {
          organization.updateComplianceStatus(newStatus);
          repository.save(organization);
        });

    log.info(
        "Compliance status updated: organizationId={}, status={}", organization.getId(), newStatus);
  }

  public Organization requireById(UUID organizationId) {
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

    Organization approved =
        auditTemplate.execute(
            organization,
            organization.getId().toString(),
            DomainEvents.Organization.APPROVED,
            () -> repository.save(organization));

    branchService.createDefaultBranch(approved);
    log.info(
        "Organization creation approved and activated: organizationId={}", organization.getId());
  }

  private void recordRejection(Organization organization, String operation, String reason) {
    organization.reject(reason);
    Organization rejected =
        auditTemplate.execute(
            organization,
            organization.getId().toString(),
            DomainEvents.Organization.REJECTED,
            () -> repository.save(organization));

    log.info("Organization {} rejected: organizationId={}", operation, rejected.getId());
  }

  private void recordUpdateApproval(Organization organization, List<FieldChange> changes) {
    changeApplier.applyChanges(organization, changes);
    Organization approved =
        auditTemplate.execute(
            organization,
            organization.getId().toString(),
            DomainEvents.Organization.APPROVED,
            () -> repository.save(organization));

    log.info(
        "Organization update approved and applied: organizationId={}, changesCount={}",
        approved.getId(),
        changes.size());
  }
}
