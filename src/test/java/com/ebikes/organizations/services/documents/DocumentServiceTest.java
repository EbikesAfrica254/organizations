package com.ebikes.organizations.services.documents;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.configurations.properties.AwsProperties;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.dtos.internal.StoredFileMetadata;
import com.ebikes.organizations.dtos.internal.UploadUrlData;
import com.ebikes.organizations.dtos.requests.documents.DocumentUploadConfirmationRequest;
import com.ebikes.organizations.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentUploadInitiationResponse;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.exceptions.BusinessRuleException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.mappers.DocumentMapper;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.audit.AuditTemplate;
import com.ebikes.organizations.support.audit.ThrowingRunnable;
import com.ebikes.organizations.support.audit.ThrowingSupplier;
import com.ebikes.organizations.support.fixtures.DocumentFixtures;
import com.ebikes.organizations.support.fixtures.DocumentRequestFixtures;
import com.ebikes.organizations.support.fixtures.DocumentResponseFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.infrastructure.WithExecutionContext;
import com.ebikes.organizations.support.makerchecker.MakerCheckerTemplate;

@DisplayName("DocumentService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class DocumentServiceTest {

  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final UUID ORGANIZATION_ID = UUID.randomUUID();
  private static final String STORAGE_KEY = "documents/KRA_PIN_CERTIFICATE/test-file.pdf";
  private static final String FILE_NAME = "test-file.pdf";
  private static final String MIME_TYPE = "application/pdf";

  @Mock private AuditTemplate auditTemplate;
  @Mock private DocumentMapper documentMapper;
  @Mock private DocumentRepository documentRepository;
  @Mock private MakerCheckerTemplate makerCheckerTemplate;
  @Mock private StorageService storageService;

  private AwsProperties awsProperties;
  private DocumentService service;
  private Organization organization;

  @BeforeEach
  void setUp() {
    AwsProperties.S3 s3 = getS3();

    awsProperties = new AwsProperties();
    awsProperties.setS3(s3);

    organization = OrganizationFixtures.active();
    ReflectionTestUtils.setField(organization, "id", ORGANIZATION_ID);

    service =
        new DocumentService(
            auditTemplate,
            awsProperties,
            documentMapper,
            documentRepository,
            makerCheckerTemplate,
            storageService);
  }

  private static AwsProperties.S3 getS3() {
    AwsProperties.S3 s3 = new AwsProperties.S3();
    s3.setAllowedContentTypes(
        Map.of(
            DocumentType.KRA_PIN_CERTIFICATE, MIME_TYPE,
            DocumentType.BUSINESS_NAME_CERTIFICATE, MIME_TYPE,
            DocumentType.PASSPORT, MIME_TYPE,
            DocumentType.CR12, MIME_TYPE));
    s3.setMaxFileSizeMb(10);
    s3.setPresignedUrlExpiryMinutes(15);
    s3.setPreviewUrlExpiryMinutes(5);
    s3.setBucketName("test-bucket");
    s3.setRegion("us-east-1");
    return s3;
  }

  @Nested
  @DisplayName("activateDocuments")
  class ActivateDocuments {

    @Test
    @DisplayName("should activate all uploaded documents and save")
    void shouldActivateAllUploadedDocumentsAndSave() {
      Document uploaded = DocumentFixtures.uploaded(organization);
      when(documentRepository.findByOrganizationIdAndStatus(
              ORGANIZATION_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of(uploaded));

      service.activateDocuments(ORGANIZATION_ID);

      assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.ACTIVE);
      verify(documentRepository).saveAll(List.of(uploaded));
    }

    @Test
    @DisplayName("should do nothing when no uploaded documents exist")
    void shouldDoNothingWhenNoUploadedDocuments() {
      when(documentRepository.findByOrganizationIdAndStatus(
              ORGANIZATION_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of());

      service.activateDocuments(ORGANIZATION_ID);

      verify(documentRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("archive")
  class Archive {

    @Test
    @DisplayName("should archive all stale uploaded documents and save")
    @SuppressWarnings("unchecked")
    void shouldArchiveAllDocumentsAndSave() {
      Document uploaded = DocumentFixtures.uploaded(organization);

      doAnswer(
              invocation -> {
                ThrowingRunnable<?> operation = invocation.getArgument(3);
                operation.run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.archive(List.of(uploaded));

      assertThat(uploaded.getStatus()).isEqualTo(DocumentStatus.ARCHIVED);
      verify(documentRepository).saveAll(List.of(uploaded));
    }

    @Test
    @DisplayName("should do nothing when list is empty")
    void shouldDoNothingWhenEmpty() {
      service.archive(List.of());

      verify(documentRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("expire")
  class Expire {

    @Test
    @DisplayName("should expire all documents and save")
    @SuppressWarnings("unchecked")
    void shouldExpireAllDocumentsAndSave() {
      Document active = DocumentFixtures.active(organization);

      doAnswer(
              invocation -> {
                ThrowingRunnable<?> operation = invocation.getArgument(3);
                operation.run();
                return null;
              })
          .when(auditTemplate)
          .execute(any(), any(), any(), any(ThrowingRunnable.class));

      service.expire(List.of(active));

      assertThat(active.getStatus()).isEqualTo(DocumentStatus.EXPIRED);
      assertThat(active.getExpiredAt()).isNotNull();
      verify(documentRepository).saveAll(List.of(active));
    }

    @Test
    @DisplayName("should do nothing when list is empty")
    void shouldDoNothingWhenEmpty() {
      service.expire(List.of());

      verify(documentRepository, never()).saveAll(anyList());
    }
  }

  @Nested
  @DisplayName("associateWithOrganization")
  class AssociateWithOrganization {

    @Test
    @DisplayName("should associate documents and save")
    void shouldAssociateDocumentsAndSave() {
      Document document = DocumentFixtures.pending(organization);
      when(documentRepository.findAllByFileStorageUrlIn(List.of(STORAGE_KEY)))
          .thenReturn(List.of(document));
      when(documentRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

      service.associateWithOrganization(List.of(STORAGE_KEY), organization);

      verify(documentRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("should throw ValidationException when storage keys list is empty")
    void shouldThrowWhenStorageKeysEmpty() {
      List<String> keys = List.of();
      assertThatThrownBy(() -> service.associateWithOrganization(keys, organization))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when duplicate storage keys provided")
    void shouldThrowWhenDuplicateStorageKeys() {
      List<String> keys = List.of(STORAGE_KEY, STORAGE_KEY);
      assertThatThrownBy(() -> service.associateWithOrganization(keys, organization))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when a key does not resolve to a document")
    void shouldThrowWhenDocumentNotFound() {
      when(documentRepository.findAllByFileStorageUrlIn(List.of(STORAGE_KEY)))
          .thenReturn(List.of());

      List<String> keys = List.of(STORAGE_KEY);
      assertThatThrownBy(() -> service.associateWithOrganization(keys, organization))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw BusinessRuleException when document is not in PENDING or UPLOADED status")
    void shouldThrowWhenDocumentInvalidStatus() {
      Document active = DocumentFixtures.active(organization);
      when(documentRepository.findAllByFileStorageUrlIn(List.of(STORAGE_KEY)))
          .thenReturn(List.of(active));

      List<String> keys = List.of(STORAGE_KEY);
      assertThatThrownBy(() -> service.associateWithOrganization(keys, organization))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("confirmUpload")
  class ConfirmUpload {

    @Test
    @DisplayName("should mark document as uploaded, publish audit event, and return response")
    @SuppressWarnings("unchecked")
    void shouldMarkUploadedAndPublishAudit() {
      Document document = DocumentFixtures.pending(organization);
      ReflectionTestUtils.setField(document, "fileStorageUrl", STORAGE_KEY);
      DocumentUploadConfirmationRequest request =
          new DocumentUploadConfirmationRequest(1024L, MIME_TYPE);
      DocumentResponse response = DocumentResponseFixtures.response(ORGANIZATION_ID);

      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
      when(storageService.getStoredFileMetadata(STORAGE_KEY))
          .thenReturn(new StoredFileMetadata(1024L, MIME_TYPE, Instant.now(), true));
      when(auditTemplate.execute(eq(document), any(), any(), any(ThrowingSupplier.class)))
          .thenAnswer(i -> document);
      when(documentMapper.toResponse(document)).thenReturn(response);

      DocumentResponse result = service.confirmUpload(DOCUMENT_ID, request);

      assertThat(result).isEqualTo(response);
      verify(auditTemplate).execute(eq(document), any(), any(), any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenNotFound() {
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());
      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmUpload(DOCUMENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when document not in PENDING status")
    void shouldThrowWhenNotPending() {
      Document uploaded = DocumentFixtures.uploaded(organization);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(uploaded));

      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmUpload(DOCUMENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when file not found in storage")
    void shouldThrowWhenFileNotInStorage() {
      Document document = DocumentFixtures.pending(organization);
      ReflectionTestUtils.setField(document, "fileStorageUrl", STORAGE_KEY);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
      when(storageService.getStoredFileMetadata(STORAGE_KEY))
          .thenReturn(new StoredFileMetadata(null, null, null, false));

      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmUpload(DOCUMENT_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when stored file mime type mismatches request")
    void shouldThrowWhenMimeTypeMismatch() {
      Document document = DocumentFixtures.pending(organization);
      ReflectionTestUtils.setField(document, "fileStorageUrl", STORAGE_KEY);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
      when(storageService.getStoredFileMetadata(STORAGE_KEY))
          .thenReturn(new StoredFileMetadata(1024L, "image/png", Instant.now(), true));

      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmUpload(DOCUMENT_ID, request))
          .isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  @DisplayName("confirmReplacement")
  class ConfirmReplacement {

    @Test
    @DisplayName("should mark replacement document as uploaded and save")
    void shouldMarkUploadedAndSave() {
      Document original = DocumentFixtures.active(organization);
      Document replacement = DocumentFixtures.pending(organization);
      ReflectionTestUtils.setField(replacement, "fileStorageUrl", STORAGE_KEY);
      ReflectionTestUtils.setField(replacement, "replacesDocument", original);
      DocumentUploadConfirmationRequest request =
          new DocumentUploadConfirmationRequest(1024L, MIME_TYPE);

      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(replacement));
      when(storageService.getStoredFileMetadata(STORAGE_KEY))
          .thenReturn(new StoredFileMetadata(1024L, MIME_TYPE, Instant.now(), true));
      when(documentRepository.save(replacement)).thenReturn(replacement);
      when(documentMapper.toResponse(replacement)).thenReturn(any());

      service.confirmReplacement(DOCUMENT_ID, request);

      assertThat(replacement.getStatus()).isEqualTo(DocumentStatus.UPLOADED);
      verify(documentRepository).save(replacement);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when document is not a replacement document")
    void shouldThrowWhenNotReplacementDocument() {
      Document document = DocumentFixtures.pending(organization);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));

      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmReplacement(DOCUMENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw BusinessRuleException when document not in PENDING status")
    void shouldThrowWhenNotPending() {
      Document uploaded = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(
          uploaded, "replacesDocument", DocumentFixtures.active(organization));
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(uploaded));

      DocumentUploadConfirmationRequest request = DocumentRequestFixtures.confirmationRequest();
      assertThatThrownBy(() -> service.confirmReplacement(DOCUMENT_ID, request))
          .isInstanceOf(BusinessRuleException.class);
    }
  }

  @Nested
  @DisplayName("download")
  class Download {

    @Test
    @DisplayName("should delegate to StorageService and return presigned URL")
    void shouldReturnPresignedUrl() {
      Document document = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(document, "fileStorageUrl", STORAGE_KEY);
      ReflectionTestUtils.setField(document, "fileName", FILE_NAME);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(document));
      when(storageService.generateDownloadUrl(anyString(), anyString(), any()))
          .thenReturn("https://presigned.url");

      String result = service.download(DOCUMENT_ID);

      assertThat(result).isEqualTo("https://presigned.url");
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when document not found")
    void shouldThrowWhenNotFound() {
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.download(DOCUMENT_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("findByOrganization")
  class FindByOrganization {

    @Test
    @DisplayName("should return all documents when includeInactive is true")
    void shouldReturnAllDocumentsWhenIncludeInactive() {
      Document active = DocumentFixtures.active(organization);
      Document replaced = DocumentFixtures.replaced(organization);
      when(documentRepository.findByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of(active, replaced));
      when(documentMapper.toSummaryResponse(any()))
          .thenReturn(DocumentResponseFixtures.summaryResponse(ORGANIZATION_ID));

      List<DocumentSummaryResponse> result = service.findByOrganization(ORGANIZATION_ID, true);

      assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("should return only ACTIVE and UPLOADED documents when includeInactive is false")
    void shouldReturnActiveAndUploadedWhenNotIncludeInactive() {
      Document active = DocumentFixtures.active(organization);
      when(documentRepository.findByOrganizationIdAndStatusIn(
              ORGANIZATION_ID, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED)))
          .thenReturn(List.of(active));
      when(documentMapper.toSummaryResponse(any()))
          .thenReturn(DocumentResponseFixtures.summaryResponse(ORGANIZATION_ID));

      List<DocumentSummaryResponse> result = service.findByOrganization(ORGANIZATION_ID, false);

      assertThat(result).hasSize(1);
    }
  }

  @Nested
  @DisplayName("findByOrganizationWithPreviews")
  class FindByOrganizationWithPreviews {

    @Test
    @DisplayName("should generate preview URLs for all documents when includeInactive is true")
    void shouldReturnPreviewsForAllWhenIncludeInactive() {
      Document active = DocumentFixtures.active(organization);
      when(documentRepository.findByOrganizationId(ORGANIZATION_ID)).thenReturn(List.of(active));
      when(storageService.generatePreviewUrl(any(), any())).thenReturn("https://preview.url");
      when(documentMapper.toPreviewResponse(any(), any(), any()))
          .thenReturn(DocumentResponseFixtures.previewResponse(ORGANIZATION_ID));

      List<DocumentPreviewResponse> result =
          service.findByOrganizationWithPreviews(ORGANIZATION_ID, true);

      assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName(
        "should generate preview URLs for only active documents when includeInactive is false")
    void shouldReturnPreviewsForActiveOnlyWhenNotIncludeInactive() {
      Document active = DocumentFixtures.active(organization);
      when(documentRepository.findByOrganizationIdAndStatusIn(
              ORGANIZATION_ID, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED)))
          .thenReturn(List.of(active));
      when(storageService.generatePreviewUrl(any(), any())).thenReturn("https://preview.url");
      when(documentMapper.toPreviewResponse(any(), any(), any()))
          .thenReturn(DocumentResponseFixtures.previewResponse(ORGANIZATION_ID));

      List<DocumentPreviewResponse> result =
          service.findByOrganizationWithPreviews(ORGANIZATION_ID, false);

      assertThat(result).hasSize(1);
    }
  }

  @Nested
  @DisplayName("getRequiredDocuments")
  class GetRequiredDocuments {

    @Test
    @DisplayName("should return correct required documents for INDIVIDUAL")
    void shouldReturnCorrectRequiredForIndividual() {
      assertThat(service.getRequiredDocuments(BusinessRegistrationType.INDIVIDUAL))
          .containsExactlyInAnyOrder(
              DocumentType.KRA_PIN_CERTIFICATE,
              DocumentType.NATIONAL_ID_FRONT,
              DocumentType.NATIONAL_ID_BACK);
    }

    @Test
    @DisplayName("should return correct required documents for SOLE_PROPRIETOR")
    void shouldReturnCorrectRequiredForSoleProprietor() {
      assertThat(service.getRequiredDocuments(BusinessRegistrationType.SOLE_PROPRIETOR))
          .containsExactlyInAnyOrder(
              DocumentType.KRA_PIN_CERTIFICATE,
              DocumentType.NATIONAL_ID_FRONT,
              DocumentType.NATIONAL_ID_BACK,
              DocumentType.BUSINESS_NAME_CERTIFICATE);
    }

    @Test
    @DisplayName("should return correct required documents for PARTNERSHIP")
    void shouldReturnCorrectRequiredForPartnership() {
      assertThat(service.getRequiredDocuments(BusinessRegistrationType.PARTNERSHIP))
          .containsExactlyInAnyOrder(
              DocumentType.KRA_PIN_CERTIFICATE,
              DocumentType.NATIONAL_ID_FRONT,
              DocumentType.NATIONAL_ID_BACK,
              DocumentType.PARTNERSHIP_DEED,
              DocumentType.BUSINESS_NAME_CERTIFICATE);
    }

    @Test
    @DisplayName("should return correct required documents for PRIVATE_LIMITED_COMPANY")
    void shouldReturnCorrectRequiredForPrivateLimitedCompany() {
      assertThat(service.getRequiredDocuments(BusinessRegistrationType.PRIVATE_LIMITED_COMPANY))
          .containsExactlyInAnyOrder(
              DocumentType.KRA_PIN_CERTIFICATE,
              DocumentType.CERTIFICATE_OF_INCORPORATION,
              DocumentType.MEMORANDUM_AND_ARTICLES,
              DocumentType.CR12);
    }

    @Test
    @DisplayName("should return correct required documents for FOREIGN_ENTITY")
    void shouldReturnCorrectRequiredForForeignEntity() {
      assertThat(service.getRequiredDocuments(BusinessRegistrationType.FOREIGN_ENTITY))
          .containsExactlyInAnyOrder(
              DocumentType.KRA_PIN_CERTIFICATE,
              DocumentType.PASSPORT,
              DocumentType.CERTIFICATE_OF_INCORPORATION,
              DocumentType.FOREIGN_COMPANY_CERTIFICATE,
              DocumentType.CR12);
    }
  }

  @Nested
  @DisplayName("hasAllRequiredDocumentsActive")
  class HasAllRequiredDocumentsActive {

    @Test
    @DisplayName("should return true when all required documents are active")
    void shouldReturnTrueWhenAllRequiredActive() {
      Document kraPin = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(kraPin, "documentType", DocumentType.KRA_PIN_CERTIFICATE);
      Document businessReg = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(
          businessReg, "documentType", DocumentType.BUSINESS_NAME_CERTIFICATE);
      Document idFront = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(idFront, "documentType", DocumentType.NATIONAL_ID_FRONT);
      Document idBack = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(idBack, "documentType", DocumentType.NATIONAL_ID_BACK);

      when(documentRepository.findByOrganizationIdAndStatus(ORGANIZATION_ID, DocumentStatus.ACTIVE))
          .thenReturn(List.of(kraPin, businessReg, idFront, idBack));

      assertThat(
              service.hasAllRequiredDocumentsActive(
                  ORGANIZATION_ID, BusinessRegistrationType.SOLE_PROPRIETOR))
          .isTrue();
    }

    @Test
    @DisplayName("should return false when a required document is missing")
    void shouldReturnFalseWhenRequiredDocumentMissing() {
      Document kraPin = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(kraPin, "documentType", DocumentType.KRA_PIN_CERTIFICATE);

      when(documentRepository.findByOrganizationIdAndStatus(ORGANIZATION_ID, DocumentStatus.ACTIVE))
          .thenReturn(List.of(kraPin));

      assertThat(
              service.hasAllRequiredDocumentsActive(
                  ORGANIZATION_ID, BusinessRegistrationType.SOLE_PROPRIETOR))
          .isFalse();
    }
  }

  @Nested
  @DisplayName("initiateUpload")
  class InitiateUpload {

    @Test
    @DisplayName(
        "should create document, assign storage key, generate upload URL, and return response")
    void shouldInitiateUploadSuccessfully() {
      Document document =
          Document.builder()
              .documentType(DocumentType.KRA_PIN_CERTIFICATE)
              .fileName(FILE_NAME)
              .mimeType(MIME_TYPE)
              .build();
      ReflectionTestUtils.setField(document, "id", DOCUMENT_ID);

      when(storageService.sanitizeFilename(FILE_NAME)).thenReturn(FILE_NAME);
      when(documentRepository.save(any(Document.class))).thenReturn(document);
      when(storageService.generateUploadUrl(anyString(), anyString(), anyLong(), anyMap()))
          .thenReturn(new UploadUrlData("https://upload.url", Map.of(), Instant.now()));
      when(documentMapper.toUploadInitiationResponse(any(), anyString(), any()))
          .thenReturn(DocumentResponseFixtures.uploadInitiationResponse());

      DocumentUploadInitiationResponse result =
          service.initiateUpload(DocumentType.KRA_PIN_CERTIFICATE, FILE_NAME, MIME_TYPE, null);

      assertThat(result).isNotNull();
      verify(storageService).generateUploadUrl(anyString(), eq(MIME_TYPE), anyLong(), anyMap());
    }

    @Test
    @DisplayName(
        "should throw ValidationException when no content type configured for document type")
    void shouldThrowWhenNoContentTypeConfigured() {
      awsProperties.getS3().setAllowedContentTypes(Map.of());

      assertThatThrownBy(
              () ->
                  service.initiateUpload(
                      DocumentType.KRA_PIN_CERTIFICATE, FILE_NAME, MIME_TYPE, null))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when content type not in allowed list")
    void shouldThrowWhenContentTypeNotAllowed() {
      awsProperties
          .getS3()
          .setAllowedContentTypes(Map.of(DocumentType.KRA_PIN_CERTIFICATE, "image/png"));

      assertThatThrownBy(
              () ->
                  service.initiateUpload(
                      DocumentType.KRA_PIN_CERTIFICATE, FILE_NAME, MIME_TYPE, null))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when expiry date required but not provided")
    void shouldThrowWhenExpiryDateRequiredButMissing() {
      assertThatThrownBy(
              () -> service.initiateUpload(DocumentType.PASSPORT, FILE_NAME, MIME_TYPE, null))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("should calculate and set conventional expiry date for CR12")
    void shouldSetConventionalExpiryForCr12() {
      ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
      Document saved =
          Document.builder()
              .documentType(DocumentType.CR12)
              .fileName(FILE_NAME)
              .mimeType(MIME_TYPE)
              .build();
      ReflectionTestUtils.setField(saved, "id", DOCUMENT_ID);

      when(storageService.sanitizeFilename(FILE_NAME)).thenReturn(FILE_NAME);
      when(documentRepository.save(captor.capture())).thenReturn(saved);
      when(storageService.generateUploadUrl(anyString(), anyString(), anyLong(), anyMap()))
          .thenReturn(new UploadUrlData("https://upload.url", Map.of(), Instant.now()));
      when(documentMapper.toUploadInitiationResponse(any(), anyString(), any()))
          .thenReturn(DocumentResponseFixtures.uploadInitiationResponse());

      service.initiateUpload(DocumentType.CR12, FILE_NAME, MIME_TYPE, null);

      LocalDate expectedExpiry =
          LocalDate.now(ZoneOffset.UTC).plus(DocumentType.CR12.conventionalExpiryPeriod());
      assertThat(captor.getAllValues()).anyMatch(d -> expectedExpiry.equals(d.getExpiryDate()));
    }
  }

  @Nested
  @DisplayName("replaceDocument")
  class ReplaceDocument {

    @Test
    @DisplayName(
        "should create replacement, mark old as replaced, publish maker-checker, and return"
            + " response")
    void shouldReplaceDocumentSuccessfully() {
      Document oldDocument = DocumentFixtures.active(organization);
      ReflectionTestUtils.setField(oldDocument, "id", UUID.randomUUID());
      ReflectionTestUtils.setField(oldDocument, "fileStorageUrl", STORAGE_KEY);

      Document newDocument =
          Document.builder()
              .documentType(DocumentType.KRA_PIN_CERTIFICATE)
              .fileName(FILE_NAME)
              .mimeType(MIME_TYPE)
              .build();
      ReflectionTestUtils.setField(newDocument, "id", DOCUMENT_ID);

      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(oldDocument));
      when(storageService.sanitizeFilename(FILE_NAME)).thenReturn(FILE_NAME);
      when(documentRepository.save(any(Document.class))).thenReturn(newDocument);
      when(storageService.generateUploadUrl(anyString(), anyString(), anyLong(), anyMap()))
          .thenReturn(new UploadUrlData("https://upload.url", Map.of(), Instant.now()));

      service.replaceDocument(DOCUMENT_ID, FILE_NAME, MIME_TYPE, null);

      verify(makerCheckerTemplate)
          .publish(any(), anyString(), eq("REPLACE_DOCUMENT"), anyList(), anyMap());
      assertThat(oldDocument.getStatus()).isEqualTo(DocumentStatus.REPLACED);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when original document not found")
    void shouldThrowWhenOriginalNotFound() {
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.replaceDocument(DOCUMENT_ID, FILE_NAME, MIME_TYPE, null))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw BusinessRuleException when original document not in ACTIVE or EXPIRED status")
    void shouldThrowWhenOriginalInvalidStatus() {
      Document pending = DocumentFixtures.pending(organization);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(pending));

      assertThatThrownBy(() -> service.replaceDocument(DOCUMENT_ID, FILE_NAME, MIME_TYPE, null))
          .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("should throw ValidationException when content type invalid for document type")
    void shouldThrowWhenContentTypeInvalid() {
      Document oldDocument = DocumentFixtures.active(organization);
      when(documentRepository.findById(DOCUMENT_ID)).thenReturn(Optional.of(oldDocument));

      assertThatThrownBy(() -> service.replaceDocument(DOCUMENT_ID, FILE_NAME, "image/png", null))
          .isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  @DisplayName("validateRequiredDocumentsUploaded")
  class ValidateRequiredDocumentsUploaded {

    @Test
    @DisplayName("should pass when all required documents are uploaded for SOLE_PROPRIETOR")
    void shouldPassWhenAllRequiredUploaded() {
      Document kraPin = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(kraPin, "documentType", DocumentType.KRA_PIN_CERTIFICATE);
      Document businessReg = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(
          businessReg, "documentType", DocumentType.BUSINESS_NAME_CERTIFICATE);
      Document idFront = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(idFront, "documentType", DocumentType.NATIONAL_ID_FRONT);
      Document idBack = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(idBack, "documentType", DocumentType.NATIONAL_ID_BACK);

      when(documentRepository.findByOrganizationIdAndStatus(
              ORGANIZATION_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of(kraPin, businessReg, idFront, idBack));

      service.validateRequiredDocumentsUploaded(
          ORGANIZATION_ID, BusinessRegistrationType.SOLE_PROPRIETOR);

      verify(documentRepository)
          .findByOrganizationIdAndStatus(ORGANIZATION_ID, DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("should pass when all required documents are uploaded for PRIVATE_LIMITED_COMPANY")
    void shouldPassWhenAllRequiredUploadedForCompany() {
      Document kraPin = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(kraPin, "documentType", DocumentType.KRA_PIN_CERTIFICATE);
      Document incorporation = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(
          incorporation, "documentType", DocumentType.CERTIFICATE_OF_INCORPORATION);
      Document memorandum = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(
          memorandum, "documentType", DocumentType.MEMORANDUM_AND_ARTICLES);
      Document cr12 = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(cr12, "documentType", DocumentType.CR12);

      when(documentRepository.findByOrganizationIdAndStatus(
              ORGANIZATION_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of(kraPin, incorporation, memorandum, cr12));

      service.validateRequiredDocumentsUploaded(
          ORGANIZATION_ID, BusinessRegistrationType.PRIVATE_LIMITED_COMPANY);

      verify(documentRepository)
          .findByOrganizationIdAndStatus(ORGANIZATION_ID, DocumentStatus.UPLOADED);
    }

    @Test
    @DisplayName("should throw ValidationException when one or more required documents are missing")
    void shouldThrowWhenRequiredDocumentsMissing() {
      Document kraPin = DocumentFixtures.uploaded(organization);
      ReflectionTestUtils.setField(kraPin, "documentType", DocumentType.KRA_PIN_CERTIFICATE);

      when(documentRepository.findByOrganizationIdAndStatus(
              ORGANIZATION_ID, DocumentStatus.UPLOADED))
          .thenReturn(List.of(kraPin));

      assertThatThrownBy(
              () ->
                  service.validateRequiredDocumentsUploaded(
                      ORGANIZATION_ID, BusinessRegistrationType.SOLE_PROPRIETOR))
          .isInstanceOf(ValidationException.class);
    }
  }
}
