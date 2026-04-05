package com.ebikes.organizations.integration.services.organizations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.ebikes.organizations.constants.EventConstants.DomainEvents;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.database.repositories.BranchRepository;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.database.repositories.OrganizationRepository;
import com.ebikes.organizations.database.repositories.OutboxRepository;
import com.ebikes.organizations.dtos.events.incoming.MakerCheckerDecision;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DocumentUploadInfo;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.fixtures.MakerCheckerFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationRequestFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractIntegrationTest;

@DisplayName("OrganizationServiceIT")
class OrganizationServiceIT extends AbstractIntegrationTest {

  @Autowired private BranchRepository branchRepository;
  @Autowired private OrganizationService organizationService;
  @Autowired private OrganizationRepository organizationRepository;
  @Autowired private DocumentRepository documentRepository;
  @Autowired private OutboxRepository outboxRepository;

  @MockitoBean private StorageService storageService;

  @BeforeEach
  void setUp() {
    outboxRepository.deleteAll();
    documentRepository.deleteAll();
    branchRepository.deleteAll();
    organizationRepository.deleteAll();
  }

  private void seedRequiredDocuments(String suffix) {
    seedPendingDocument(
        DocumentType.KRA_PIN_CERTIFICATE, storageKey(DocumentType.KRA_PIN_CERTIFICATE, suffix));
    seedPendingDocument(
        DocumentType.CERTIFICATE_OF_INCORPORATION,
        storageKey(DocumentType.CERTIFICATE_OF_INCORPORATION, suffix));
    seedPendingDocument(
        DocumentType.MEMORANDUM_AND_ARTICLES,
        storageKey(DocumentType.MEMORANDUM_AND_ARTICLES, suffix));
    seedPendingDocument(DocumentType.CR12, storageKey(DocumentType.CR12, suffix));
  }

  private void seedPendingDocument(DocumentType documentType, String storageKey) {
    Document document =
        Document.builder()
            .documentType(documentType)
            .fileStorageUrl(storageKey)
            .status(DocumentStatus.UPLOADED)
            .build();
    documentRepository.save(document);
  }

  private static String storageKey(DocumentType documentType, String suffix) {
    return "documents/" + documentType.name() + "/test-doc-" + suffix + ".pdf";
  }

  private CreateOrganizationRequest createRequest() {
    return new CreateOrganizationRequest(
        List.of(
            new Address(
                AddressTag.PRIMARY,
                "Nairobi",
                "Kenya",
                BigDecimal.valueOf(-1.2921),
                BigDecimal.valueOf(36.8219),
                "00100",
                "Kenyatta Avenue")),
        List.of(
            new DocumentUploadInfo(
                DocumentType.KRA_PIN_CERTIFICATE,
                null,
                "kra-pin.pdf",
                1024L,
                "application/pdf",
                storageKey(DocumentType.KRA_PIN_CERTIFICATE, "1")),
            new DocumentUploadInfo(
                DocumentType.CERTIFICATE_OF_INCORPORATION,
                null,
                "incorporation.pdf",
                1024L,
                "application/pdf",
                storageKey(DocumentType.CERTIFICATE_OF_INCORPORATION, "1")),
            new DocumentUploadInfo(
                DocumentType.MEMORANDUM_AND_ARTICLES,
                null,
                "memorandum.pdf",
                1024L,
                "application/pdf",
                storageKey(DocumentType.MEMORANDUM_AND_ARTICLES, "1")),
            new DocumentUploadInfo(
                DocumentType.CR12,
                null,
                "cr12.pdf",
                1024L,
                "application/pdf",
                storageKey(DocumentType.CR12, "1"))),
        "Test Organization",
        "test@ebikes.test",
        null,
        null,
        "Test Legal Name " + UUID.randomUUID(),
        SecurityFixtures.TEST_USER_ID,
        SecurityFixtures.TEST_PHONE_NUMBER,
        null,
        BusinessRegistrationType.PRIVATE_LIMITED_COMPANY);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should persist organization with PENDING_APPROVAL status")
    void shouldPersistOrganizationWithPendingApprovalStatus() {
      seedRequiredDocuments("1");
      CreateOrganizationRequest request = createRequest();

      Organization result = organizationService.create(request);

      assertThat(result.getId()).isNotNull();
      assertThat(result.getStatus()).isEqualTo(OrganizationStatus.PENDING_APPROVAL);
      assertThat(result.getLegalName()).isEqualTo(request.legalName());
      assertThat(organizationRepository.existsById(result.getId())).isTrue();
    }

    @Test
    @DisplayName("should write a maker-checker outbox record on create")
    void shouldWriteMakerCheckerOutboxRecord() {
      seedRequiredDocuments("1");

      organizationService.create(createRequest());

      List<com.ebikes.organizations.database.entities.Outbox> outboxRecords =
          outboxRepository.findAll();
      assertThat(outboxRecords)
          .hasSize(1)
          .first()
          .satisfies(
              outbox -> {
                assertThat(outbox.getEventType()).isEqualTo("ORGANIZATION");
                assertThat(outbox.getStatus()).isEqualTo(OutboxStatus.PENDING);
                assertThat(outbox.getRoutingKey())
                    .isEqualTo("organizations.organization.maker-checker-request");
              });
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when legal name already exists")
    void shouldThrowOnDuplicateLegalName() {
      seedRequiredDocuments("1");
      CreateOrganizationRequest request = createRequest();
      organizationService.create(request);

      seedRequiredDocuments("2");
      CreateOrganizationRequest duplicate =
          new CreateOrganizationRequest(
              request.addresses(),
              List.of(
                  new DocumentUploadInfo(
                      DocumentType.KRA_PIN_CERTIFICATE,
                      null,
                      "kra-pin.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.KRA_PIN_CERTIFICATE, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CERTIFICATE_OF_INCORPORATION,
                      null,
                      "incorporation.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CERTIFICATE_OF_INCORPORATION, "2")),
                  new DocumentUploadInfo(
                      DocumentType.MEMORANDUM_AND_ARTICLES,
                      null,
                      "memorandum.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.MEMORANDUM_AND_ARTICLES, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CR12,
                      null,
                      "cr12.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CR12, "2"))),
              request.displayName(),
              request.email(),
              null,
              null,
              request.legalName(), // same legal name
              request.ownerId(),
              request.phoneNumber(),
              null,
              request.registrationType());

      assertThatThrownBy(() -> organizationService.create(duplicate))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should submit update for approval and write maker-checker outbox record")
    void shouldSubmitUpdateForApproval() {
      seedRequiredDocuments("1");
      Organization org = organizationService.create(createRequest());
      outboxRepository.deleteAll();

      org = organizationRepository.findById(org.getId()).orElseThrow();
      org.reject("test");
      organizationRepository.save(org);

      UpdateOrganizationRequest request = OrganizationRequestFixtures.update();

      Organization result = organizationService.update(org.getId(), request);

      assertThat(result.getStatus()).isEqualTo(OrganizationStatus.PENDING_APPROVAL);
      List<com.ebikes.organizations.database.entities.Outbox> outboxRecords =
          outboxRepository.findAll();
      assertThat(outboxRecords)
          .hasSize(1)
          .first()
          .satisfies(
              outbox -> {
                assertThat(outbox.getEventType()).isEqualTo("ORGANIZATION");
                assertThat(outbox.getRoutingKey())
                    .isEqualTo("organizations.organization.maker-checker-request");
              });
    }

    @Test
    @DisplayName("should return existing organization unchanged when no changes detected")
    void shouldReturnUnchangedWhenNoChangesDetected() {
      seedRequiredDocuments("1");
      Organization org = organizationService.create(createRequest());
      org = organizationRepository.findById(org.getId()).orElseThrow();
      org.reject("test");
      organizationRepository.save(org);
      outboxRepository.deleteAll();

      UpdateOrganizationRequest noOp =
          new UpdateOrganizationRequest(
              null, null, null, null, null, null, org.getLegalName(), null, null, null);

      organizationService.update(org.getId(), noOp);

      assertThat(outboxRepository.findAll()).isEmpty();
    }

    @Test
    @DisplayName("should throw DuplicateResourceException when new legal name already taken")
    void shouldThrowOnDuplicateLegalName() {
      seedRequiredDocuments("1");
      Organization first = organizationService.create(createRequest());

      seedRequiredDocuments("2");
      CreateOrganizationRequest secondRequest =
          new CreateOrganizationRequest(
              createRequest().addresses(),
              List.of(
                  new DocumentUploadInfo(
                      DocumentType.KRA_PIN_CERTIFICATE,
                      null,
                      "kra-pin.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.KRA_PIN_CERTIFICATE, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CERTIFICATE_OF_INCORPORATION,
                      null,
                      "incorporation.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CERTIFICATE_OF_INCORPORATION, "2")),
                  new DocumentUploadInfo(
                      DocumentType.MEMORANDUM_AND_ARTICLES,
                      null,
                      "memorandum.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.MEMORANDUM_AND_ARTICLES, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CR12,
                      null,
                      "cr12.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CR12, "2"))),
              "Second Org",
              "second@ebikes.test",
              null,
              null,
              "Second Legal Name " + UUID.randomUUID(),
              SecurityFixtures.TEST_USER_ID,
              SecurityFixtures.TEST_PHONE_NUMBER,
              null,
              BusinessRegistrationType.PRIVATE_LIMITED_COMPANY);
      organizationService.create(secondRequest);

      Organization secondOrg =
          organizationRepository.findAll().stream()
              .filter(o -> o.getLegalName().startsWith("Second Legal Name"))
              .findFirst()
              .orElseThrow();
      secondOrg.reject("test");
      organizationRepository.save(secondOrg);

      UpdateOrganizationRequest clashingUpdate =
          new UpdateOrganizationRequest(
              null, null, null, null, null, null, first.getLegalName(), null, null, null);

      UUID secondOrgId = secondOrg.getId();
      assertThatThrownBy(() -> organizationService.update(secondOrgId, clashingUpdate))
          .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when organization does not exist")
    void shouldThrowWhenOrganizationNotFound() {
      UUID nonExistentId = UUID.randomUUID();
      UpdateOrganizationRequest request = OrganizationRequestFixtures.update();
      assertThatThrownBy(() -> organizationService.update(nonExistentId, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("handleApprovalDecision")
  class HandleApprovalDecision {

    private Organization pendingOrg;

    @BeforeEach
    void setUp() {
      seedRequiredDocuments("1");
      pendingOrg = organizationService.create(createRequest());
      outboxRepository.deleteAll();
    }

    @Nested
    @DisplayName("CREATE operation")
    class CreateOperation {

      @Test
      @DisplayName("approved: should activate organization and publish OrganizationCreatedEvent")
      void shouldActivateOrganizationOnApproval() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.approved(pendingOrg.getId(), "ORGANIZATION", "CREATE");

        organizationService.handleApprovalDecision(decision);

        Organization updated = organizationRepository.findById(pendingOrg.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
        assertThat(updated.getApprovedAt()).isNotNull();
        assertThat(updated.getActivatedAt()).isNotNull();
      }

      @Test
      @DisplayName("approved: should write OrganizationCreatedEvent and audit outbox records")
      void shouldWriteCreatedEventAndAuditToOutbox() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.approved(pendingOrg.getId(), "ORGANIZATION", "CREATE");

        organizationService.handleApprovalDecision(decision);

        List<com.ebikes.organizations.database.entities.Outbox> outboxRecords =
            outboxRepository.findAll();
        assertThat(outboxRecords)
            .hasSizeGreaterThanOrEqualTo(2)
            .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.CREATED))
            .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.APPROVED));
      }

      @Test
      @DisplayName("rejected: should set organization status to REJECTED")
      void shouldRejectOrganization() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.rejected(
                pendingOrg.getId(), "ORGANIZATION", "CREATE", "Docs incomplete");

        organizationService.handleApprovalDecision(decision);

        Organization updated = organizationRepository.findById(pendingOrg.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OrganizationStatus.REJECTED);
      }

      @Test
      @DisplayName("rejected: should write audit outbox record")
      void shouldWriteAuditOutboxRecordOnRejection() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.rejected(
                pendingOrg.getId(), "ORGANIZATION", "CREATE", "Docs incomplete");

        organizationService.handleApprovalDecision(decision);

        assertThat(outboxRepository.findAll())
            .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.REJECTED));
      }
    }

    @Nested
    @DisplayName("UPDATE operation")
    class UpdateOperation {

      private Organization activeOrg;

      @BeforeEach
      void setUp() {
        pendingOrg.reject("setup-rejection");
        organizationRepository.save(pendingOrg);
        outboxRepository.deleteAll();

        activeOrg =
            organizationService.update(pendingOrg.getId(), OrganizationRequestFixtures.update());
        activeOrg = organizationRepository.findById(activeOrg.getId()).orElseThrow();
        outboxRepository.deleteAll();
      }

      @Test
      @DisplayName("approved: should write OrganizationUpdatedEvent to outbox")
      void shouldWriteUpdatedEventOnApproval() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.approved(activeOrg.getId(), "ORGANIZATION", "UPDATE");

        organizationService.handleApprovalDecision(decision);

        assertThat(outboxRepository.findAll())
            .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.UPDATED));
      }

      @Test
      @DisplayName("rejected: should write audit outbox record")
      void shouldWriteAuditOutboxRecordOnRejection() {
        MakerCheckerDecision decision =
            MakerCheckerFixtures.rejected(
                activeOrg.getId(), "ORGANIZATION", "UPDATE", "Invalid data");

        organizationService.handleApprovalDecision(decision);

        assertThat(outboxRepository.findAll())
            .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.REJECTED));
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown operation")
    void shouldThrowForUnknownOperation() {
      MakerCheckerDecision decision =
          MakerCheckerFixtures.approved(pendingOrg.getId(), "ORGANIZATION", "DELETE");

      assertThatThrownBy(() -> organizationService.handleApprovalDecision(decision))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("deactivate")
  class Deactivate {

    @Test
    @DisplayName("should deactivate organization and write audit outbox record")
    void shouldDeactivateOrganizationAndWriteAuditRecord() {
      seedRequiredDocuments("1");
      Organization org = organizationService.create(createRequest());
      organizationService.handleApprovalDecision(
          MakerCheckerFixtures.approved(org.getId(), "ORGANIZATION", "CREATE"));
      outboxRepository.deleteAll();

      Organization result = organizationService.deactivate(org.getId(), "Business closed");

      assertThat(result.getStatus()).isEqualTo(OrganizationStatus.DEACTIVATED);
      assertThat(result.getDeactivatedAt()).isNotNull();
      assertThat(outboxRepository.findAll())
          .anyMatch(o -> o.getEventType().equals(DomainEvents.Organization.DEACTIVATED));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when organization does not exist")
    void shouldThrowWhenNotFound() {
      UUID nonExistentId = UUID.randomUUID();
      assertThatThrownBy(() -> organizationService.deactivate(nonExistentId, "reason"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return organization when found")
    void shouldReturnOrganizationWhenFound() {
      seedRequiredDocuments("1");
      Organization org = organizationService.create(createRequest());

      Organization result = organizationService.findById(org.getId());

      assertThat(result.getId()).isEqualTo(org.getId());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      UUID nonExistentId = UUID.randomUUID();
      assertThatThrownBy(() -> organizationService.findById(nonExistentId))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("search")
  class Search {

    @Test
    @DisplayName("should return paginated results scoped to current user")
    void shouldReturnPaginatedResultsScopedToCurrentUser() {
      seedRequiredDocuments("1");
      organizationService.create(createRequest());

      seedRequiredDocuments("2");
      organizationService.create(
          new CreateOrganizationRequest(
              createRequest().addresses(),
              List.of(
                  new DocumentUploadInfo(
                      DocumentType.KRA_PIN_CERTIFICATE,
                      null,
                      "kra-pin.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.KRA_PIN_CERTIFICATE, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CERTIFICATE_OF_INCORPORATION,
                      null,
                      "incorporation.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CERTIFICATE_OF_INCORPORATION, "2")),
                  new DocumentUploadInfo(
                      DocumentType.MEMORANDUM_AND_ARTICLES,
                      null,
                      "memorandum.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.MEMORANDUM_AND_ARTICLES, "2")),
                  new DocumentUploadInfo(
                      DocumentType.CR12,
                      null,
                      "cr12.pdf",
                      1024L,
                      "application/pdf",
                      storageKey(DocumentType.CR12, "2"))),
              "Second Org",
              "second@ebikes.test",
              null,
              null,
              "Second Legal Name " + UUID.randomUUID(),
              SecurityFixtures.TEST_USER_ID,
              SecurityFixtures.TEST_PHONE_NUMBER,
              null,
              BusinessRegistrationType.PRIVATE_LIMITED_COMPANY));

      OrganizationFilter filter = new OrganizationFilter();
      var page = organizationService.search(filter);

      assertThat(page.getContent()).hasSize(2);
      assertThat(page.getContent())
          .allMatch(o -> o.getCreatedBy().equals(SecurityFixtures.TEST_USER_ID));
    }
  }
}
