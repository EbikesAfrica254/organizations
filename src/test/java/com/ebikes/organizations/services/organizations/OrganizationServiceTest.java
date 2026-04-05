package com.ebikes.organizations.services.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.constants.EventConstants.DomainEvents;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.OrganizationRepository;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationReference;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.FieldType;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.mappers.EventMapper;
import com.ebikes.organizations.services.branches.BranchService;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.services.events.OutboxService;
import com.ebikes.organizations.support.audit.AuditTemplate;
import com.ebikes.organizations.support.audit.ThrowingRunnable;
import com.ebikes.organizations.support.audit.ThrowingSupplier;
import com.ebikes.organizations.support.changes.ChangeApplier;
import com.ebikes.organizations.support.changes.SnapshotCreator;
import com.ebikes.organizations.support.fixtures.MakerCheckerFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationRequestFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.WithExecutionContext;
import com.ebikes.organizations.support.makerchecker.MakerCheckerTemplate;

@DisplayName("OrganizationService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class OrganizationServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString(SecurityFixtures.TEST_ORGANIZATION_ID);
  private static final String ENTITY_TYPE = "Organization";

  @Mock private AuditTemplate auditTemplate;
  @Mock private BranchService branchService;
  @Mock private ChangeApplier changeApplier;
  @Mock private DocumentService documentService;
  @Mock private EventMapper eventMapper;
  @Mock private MakerCheckerTemplate makerCheckerTemplate;
  @Mock private OrganizationRepository repository;
  @Mock private OutboxService outboxService;
  @Mock private SnapshotCreator snapshotCreator;

  private OrganizationService service;
  private Organization organization;

  @BeforeEach
  void setUp() {
    service =
        new OrganizationService(
            auditTemplate,
            branchService,
            changeApplier,
            documentService,
            eventMapper,
            makerCheckerTemplate,
            repository,
            outboxService,
            snapshotCreator);

    organization = OrganizationFixtures.pendingApproval();
    ReflectionTestUtils.setField(organization, "id", ORGANIZATION_ID);
  }

  @SuppressWarnings("unchecked")
  private void stubAuditExecute(Organization returned) {
    doAnswer(
            inv -> {
              ThrowingSupplier<?, ?> supplier = inv.getArgument(3);
              supplier.get();
              return returned;
            })
        .when(auditTemplate)
        .execute(any(Organization.class), anyString(), anyString(), any(ThrowingSupplier.class));
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName(
        "should save organization, associate documents, re-fetch, publish maker-checker, return"
            + " organization")
    void shouldCreateOrganizationSuccessfully() {
      List<FieldChange> snapshot =
          List.of(new FieldChange("legalName", FieldType.STRING, "Test Organization Ltd", null));

      when(repository.existsByLegalName(any())).thenReturn(false);
      when(repository.save(any(Organization.class))).thenReturn(organization);
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));
      when(snapshotCreator.extractFields(organization)).thenReturn(snapshot);

      Organization result = service.create(OrganizationRequestFixtures.create());

      assertThat(result).isEqualTo(organization);
      verify(documentService).associateWithOrganization(anyList(), eq(organization));
      verify(snapshotCreator).extractFields(organization);
      verify(makerCheckerTemplate)
          .publish(organization, ORGANIZATION_ID.toString(), "CREATE", snapshot);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when legal name already exists")
    void shouldThrowWhenLegalNameDuplicate() {
      when(repository.existsByLegalName(any())).thenReturn(true);

      CreateOrganizationRequest request = OrganizationRequestFixtures.create();
      assertThatThrownBy(() -> service.create(request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("deactivate")
  class Deactivate {

    @Test
    @DisplayName("should deactivate organization, publish audit event, return saved organization")
    @SuppressWarnings("unchecked")
    void shouldDeactivateSuccessfully() {
      Organization active = OrganizationFixtures.active();
      ReflectionTestUtils.setField(active, "id", ORGANIZATION_ID);

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(active));
      when(repository.save(any(Organization.class))).thenReturn(active);
      stubAuditExecute(active);

      Organization result = service.deactivate(ORGANIZATION_ID, "test reason");

      assertThat(result.getStatus()).isEqualTo(OrganizationStatus.DEACTIVATED);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.DEACTIVATED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when organization not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deactivate(ORGANIZATION_ID, "reason"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return organization when found")
    void shouldReturnOrganizationWhenFound() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));

      assertThat(service.findById(ORGANIZATION_ID)).isEqualTo(organization);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(ORGANIZATION_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findReferencesByIds")
  class FindReferencesByIds {

    @Test
    @DisplayName("should delegate to repository and return result")
    void shouldReturnReferences() {
      List<UUID> ids = List.of(ORGANIZATION_ID);
      OrganizationReference ref = new OrganizationReference(ORGANIZATION_ID, "Test Organization");
      when(repository.findByIdIn(ids)).thenReturn(List.of(ref));

      assertThat(service.findReferencesByIds(ids)).containsExactly(ref);
    }
  }

  @Nested
  @DisplayName("handleApprovalDecision")
  class HandleApprovalDecision {

    @Test
    @DisplayName(
        "CREATE APPROVED — validates documents, activates, approves, creates default branch")
    @SuppressWarnings("unchecked")
    void shouldHandleCreateApproved() {
      Organization pending = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(pending, "id", ORGANIZATION_ID);
      Organization approved = OrganizationFixtures.active();
      ReflectionTestUtils.setField(approved, "id", ORGANIZATION_ID);

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(pending));
      when(repository.save(any(Organization.class))).thenReturn(approved);
      stubAuditExecute(approved);

      service.handleApprovalDecision(
          MakerCheckerFixtures.approved(ORGANIZATION_ID, ENTITY_TYPE, "CREATE"));

      verify(documentService)
          .validateRequiredDocumentsUploaded(
              eq(ORGANIZATION_ID), any(BusinessRegistrationType.class));
      verify(documentService).activateDocuments(ORGANIZATION_ID);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.APPROVED),
              any(ThrowingSupplier.class));
      verify(branchService).createDefaultBranch(approved);
    }

    @Test
    @DisplayName("CREATE REJECTED — rejects organization, publishes audit event")
    @SuppressWarnings("unchecked")
    void shouldHandleCreateRejected() {
      Organization pending = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(pending, "id", ORGANIZATION_ID);
      Organization rejected = OrganizationFixtures.rejected("not compliant");
      ReflectionTestUtils.setField(rejected, "id", ORGANIZATION_ID);

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(pending));
      when(repository.save(any(Organization.class))).thenReturn(rejected);
      stubAuditExecute(rejected);

      service.handleApprovalDecision(
          MakerCheckerFixtures.rejected(ORGANIZATION_ID, ENTITY_TYPE, "CREATE", "not compliant"));

      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.REJECTED),
              any(ThrowingSupplier.class));
      verify(branchService, never()).createDefaultBranch(any());
    }

    @Test
    @DisplayName("UPDATE APPROVED — applies changes, publishes audit event")
    @SuppressWarnings("unchecked")
    void shouldHandleUpdateApproved() {
      Organization pending = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(pending, "id", ORGANIZATION_ID);
      Organization approved = OrganizationFixtures.active();
      ReflectionTestUtils.setField(approved, "id", ORGANIZATION_ID);

      List<FieldChange> changes =
          List.of(new FieldChange("legalName", FieldType.STRING, "New Name Ltd", "Old Name Ltd"));

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(pending));
      when(repository.save(any(Organization.class))).thenReturn(approved);
      stubAuditExecute(approved);

      service.handleApprovalDecision(
          MakerCheckerFixtures.approved(ORGANIZATION_ID, ENTITY_TYPE, "UPDATE", changes));

      verify(changeApplier).applyChanges(pending, changes);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.APPROVED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("UPDATE REJECTED — rejects organization, publishes audit event")
    @SuppressWarnings("unchecked")
    void shouldHandleUpdateRejected() {
      Organization pending = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(pending, "id", ORGANIZATION_ID);
      Organization rejected = OrganizationFixtures.rejected("invalid documents");
      ReflectionTestUtils.setField(rejected, "id", ORGANIZATION_ID);

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(pending));
      when(repository.save(any(Organization.class))).thenReturn(rejected);
      stubAuditExecute(rejected);

      service.handleApprovalDecision(
          MakerCheckerFixtures.rejected(
              ORGANIZATION_ID, ENTITY_TYPE, "UPDATE", "invalid documents"));

      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.REJECTED),
              any(ThrowingSupplier.class));
      verify(changeApplier, never()).applyChanges(any(), anyList());
    }

    @Test
    @DisplayName("unknown operation — throws IllegalArgumentException")
    void shouldThrowOnUnknownOperation() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(organization));

      MakerCheckerDecision unknown =
          MakerCheckerFixtures.approved(ORGANIZATION_ID, ENTITY_TYPE, "UNKNOWN_OP");

      assertThatThrownBy(() -> service.handleApprovalDecision(unknown))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("organization not found — throws ResourceNotFoundException")
    void shouldThrowWhenOrganizationNotFound() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(ORGANIZATION_ID, ENTITY_TYPE, "CREATE");
      assertThatThrownBy(() -> service.handleApprovalDecision(decision))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should delegate to repository with built spec and pageable, return page")
    @SuppressWarnings("unchecked")
    void shouldReturnPageOfOrganizations() {
      Organization active = OrganizationFixtures.active();
      ReflectionTestUtils.setField(active, "id", ORGANIZATION_ID);
      Page<Organization> page = new PageImpl<>(List.of(active));

      when(repository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

      Page<Organization> result = service.search(new OrganizationFilter());

      assertThat(result.getContent()).containsExactly(active);
      verify(repository).findAll(any(Specification.class), any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName(
        "should detect changes, resubmit, save, publish maker-checker, return organization")
    void shouldUpdateOrganizationSuccessfully() {
      Organization existing = OrganizationFixtures.rejected("old rejection");
      ReflectionTestUtils.setField(existing, "id", ORGANIZATION_ID);

      List<FieldChange> changes =
          List.of(
              new FieldChange(
                  "legalName", FieldType.STRING, "New Legal Name Ltd", "Test Organization Ltd"));

      UpdateOrganizationRequest request = OrganizationRequestFixtures.update("New Legal Name Ltd");

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
      when(repository.existsByLegalName(request.legalName())).thenReturn(false);
      when(repository.save(any(Organization.class))).thenReturn(existing);
      when(snapshotCreator.extractChanges(request, existing)).thenReturn(changes);

      Organization result = service.update(ORGANIZATION_ID, request);

      assertThat(result).isEqualTo(existing);
      verify(makerCheckerTemplate).publish(existing, ORGANIZATION_ID.toString(), "UPDATE", changes);
    }

    @Test
    @DisplayName("should return existing organization unchanged when no changes detected")
    void shouldReturnUnchangedWhenNoChanges() {
      Organization existing = OrganizationFixtures.active();
      ReflectionTestUtils.setField(existing, "id", ORGANIZATION_ID);

      UpdateOrganizationRequest request = OrganizationRequestFixtures.update();

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
      when(repository.existsByLegalName(anyString())).thenReturn(false);

      Organization result = service.update(ORGANIZATION_ID, request);

      assertThat(result).isEqualTo(existing);
      verify(makerCheckerTemplate, never()).publish(any(), any(), any(), anyList());
      verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when organization not found")
    void shouldThrowWhenNotFound() {
      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.empty());

      UpdateOrganizationRequest request = OrganizationRequestFixtures.update();

      assertThatThrownBy(() -> service.update(ORGANIZATION_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when new legal name conflicts")
    void shouldThrowWhenLegalNameConflicts() {
      Organization existing = OrganizationFixtures.active();
      ReflectionTestUtils.setField(existing, "id", ORGANIZATION_ID);

      UpdateOrganizationRequest request =
          OrganizationRequestFixtures.update("Conflicting Name Ltd");

      when(repository.findById(ORGANIZATION_ID)).thenReturn(Optional.of(existing));
      when(repository.existsByLegalName(request.legalName())).thenReturn(true);

      assertThatThrownBy(() -> service.update(ORGANIZATION_ID, request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("updateComplianceStatus")
  class UpdateComplianceStatus {

    @Test
    @DisplayName("should set COMPLIANT and publish audit when all required docs active")
    void shouldSetCompliantWhenAllRequiredDocsActive() {
      Organization org = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(org, "id", ORGANIZATION_ID);
      ReflectionTestUtils.setField(org, "complianceStatus", ComplianceStatus.NON_COMPLIANT);

      when(documentService.hasAllRequiredDocumentsActive(
              ORGANIZATION_ID, org.getRegistrationType()))
          .thenReturn(true);

      doAnswer(
              inv -> {
                ThrowingRunnable<?> runnable = inv.getArgument(3);
                runnable.run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), anyString(), anyString(), any(ThrowingRunnable.class));

      service.updateComplianceStatus(org);

      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.COMPLIANCE_UPDATED),
              any(ThrowingRunnable.class));
    }

    @Test
    @DisplayName("should set NON_COMPLIANT and publish audit when required docs missing")
    void shouldSetNonCompliantWhenRequiredDocsMissing() {
      Organization org = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(org, "id", ORGANIZATION_ID);
      ReflectionTestUtils.setField(org, "complianceStatus", ComplianceStatus.COMPLIANT);

      when(documentService.hasAllRequiredDocumentsActive(
              ORGANIZATION_ID, org.getRegistrationType()))
          .thenReturn(false);

      doAnswer(
              inv -> {
                ThrowingRunnable<?> runnable = inv.getArgument(3);
                runnable.run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), anyString(), anyString(), any(ThrowingRunnable.class));

      service.updateComplianceStatus(org);

      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Organization.COMPLIANCE_UPDATED),
              any(ThrowingRunnable.class));
    }

    @Test
    @DisplayName("should skip audit when compliance status unchanged")
    void shouldSkipWhenStatusUnchanged() {
      // OrganizationFixtures.active() has NON_COMPLIANT default now
      // so returning false means no change — no audit event
      Organization org = OrganizationFixtures.active();
      ReflectionTestUtils.setField(org, "id", ORGANIZATION_ID);

      when(documentService.hasAllRequiredDocumentsActive(
              ORGANIZATION_ID, org.getRegistrationType()))
          .thenReturn(false);

      service.updateComplianceStatus(org);

      verify(auditTemplate, never()).execute(any(), any(), any(), any(ThrowingSupplier.class));
    }
  }
}
