package com.ebikes.organizations.services.documents;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.configurations.properties.AwsProperties;
import com.ebikes.organizations.constants.EventConstants.DomainEvents;
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
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.BusinessRuleException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.mappers.DocumentMapper;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.audit.AuditTemplate;
import com.ebikes.organizations.support.makerchecker.MakerCheckerTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class DocumentService {

  private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
  private static final String DOCUMENT_TYPE = "documentType";

  private final AuditTemplate auditTemplate;
  private final AwsProperties awsProperties;
  private final DocumentMapper documentMapper;
  private final DocumentRepository documentRepository;
  private final MakerCheckerTemplate makerCheckerTemplate;
  private final StorageService storageService;

  @Transactional
  public void activateDocuments(UUID organizationId) {
    log.info("Activating documents for organization: organizationId={}", organizationId);

    List<Document> uploadedDocuments =
        documentRepository.findByOrganizationIdAndStatus(organizationId, DocumentStatus.UPLOADED);

    if (uploadedDocuments.isEmpty()) {
      log.debug("No uploaded documents to activate for organization: {}", organizationId);
      return;
    }

    uploadedDocuments.forEach(Document::activate);
    documentRepository.saveAll(uploadedDocuments);

    log.info(
        "Activated {} documents for organization: {}", uploadedDocuments.size(), organizationId);
  }

  @Transactional
  public void archive(List<Document> documents) {
    if (documents.isEmpty()) {
      return;
    }
    documents.forEach(
        document ->
            auditTemplate.execute(
                document,
                document.getOrganization() != null
                    ? document.getOrganization().getId().toString()
                    : null,
                DomainEvents.Document.ARCHIVED,
                document::markArchived));
    documentRepository.saveAll(documents);
    log.info("Archived {} stale uploaded documents", documents.size());
  }

  @Transactional
  public void associateWithOrganization(List<String> storageKeys, Organization organization) {
    log.info(
        "Associating documents with organization: organizationId={}, documentCount={}",
        organization.getId(),
        storageKeys.size());

    if (storageKeys.isEmpty()) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "At least one document is required for association",
          "documents",
          storageKeys.toArray());
    }

    Set<String> uniqueStorageKeys = Set.copyOf(storageKeys);
    if (uniqueStorageKeys.size() != storageKeys.size()) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "Duplicate document storage keys provided",
          "documents",
          storageKeys.toArray());
    }

    List<Document> documents = documentRepository.findAllByFileStorageUrlIn(storageKeys);

    if (documents.size() != storageKeys.size()) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND,
          "One or more documents could not be found for association");
    }

    documents.forEach(
        document -> {
          validateDocumentStatus(
              document, List.of(DocumentStatus.PENDING, DocumentStatus.UPLOADED), "associate");
          document.associate(organization);
        });

    List<Document> savedDocuments = documentRepository.saveAll(documents);

    log.info(
        "Associated {} documents with organization: organizationId={}",
        savedDocuments.size(),
        organization.getId());
  }

  @Transactional
  public DocumentResponse confirmReplacement(
      UUID documentId, DocumentUploadConfirmationRequest request) {
    log.info("Confirming document replacement upload: documentId={}", documentId);

    Document newDocument = requireById(documentId);
    validateIsReplacementDocument(newDocument);
    validateUploadConfirmation(newDocument, request);
    newDocument.markUploaded(request.fileSizeBytes(), request.mimeType());
    Document saved = documentRepository.save(newDocument);

    log.info("Document replacement upload confirmed, pending approval: documentId={}", documentId);

    return documentMapper.toResponse(saved);
  }

  @Transactional
  public DocumentResponse confirmUpload(
      UUID documentId, DocumentUploadConfirmationRequest request) {

    Document document = requireById(documentId);
    validateUploadConfirmation(document, request);
    document.markUploaded(request.fileSizeBytes(), request.mimeType());

    Document uploadedDocument =
        auditTemplate.execute(
            document,
            document.getOrganization() != null
                ? document.getOrganization().getId().toString()
                : null,
            DomainEvents.Document.UPLOADED,
            () -> documentRepository.save(document));

    log.info("Document upload confirmed: documentId={}", documentId);

    return documentMapper.toResponse(uploadedDocument);
  }

  @Transactional(readOnly = true)
  public String download(UUID documentId) {
    log.info("Generating download URL: documentId={}", documentId);

    Document document = requireById(documentId);
    Duration ttl = Duration.ofMinutes(awsProperties.getS3().getPresignedUrlExpiryMinutes());

    return storageService.generateDownloadUrl(
        document.getFileStorageUrl(), document.getFileName(), ttl);
  }

  @Transactional
  public void expire(List<Document> documents) {
    if (documents.isEmpty()) {
      return;
    }
    documents.forEach(
        document ->
            auditTemplate.execute(
                document,
                document.getOrganization() != null
                    ? document.getOrganization().getId().toString()
                    : null,
                DomainEvents.Document.EXPIRED,
                document::markExpired));
    documentRepository.saveAll(documents);
    log.info("Expired {} documents", documents.size());
  }

  @Transactional(readOnly = true)
  public List<DocumentSummaryResponse> findByOrganization(
      UUID organizationId, boolean includeInactive) {
    log.info(
        "Finding documents: organizationId={}, includeInactive={}",
        organizationId,
        includeInactive);

    List<Document> documents =
        includeInactive
            ? documentRepository.findByOrganizationId(organizationId)
            : documentRepository.findByOrganizationIdAndStatusIn(
                organizationId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED));

    log.debug("Found {} documents for organization: {}", documents.size(), organizationId);

    return documents.stream().map(documentMapper::toSummaryResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<DocumentPreviewResponse> findByOrganizationWithPreviews(
      UUID organizationId, boolean includeInactive) {
    log.info(
        "Finding documents with preview URLs: organizationId={}, includeInactive={}",
        organizationId,
        includeInactive);

    List<Document> documents =
        includeInactive
            ? documentRepository.findByOrganizationId(organizationId)
            : documentRepository.findByOrganizationIdAndStatusIn(
                organizationId, List.of(DocumentStatus.ACTIVE, DocumentStatus.UPLOADED));

    Duration previewTtl = Duration.ofMinutes(awsProperties.getS3().getPreviewUrlExpiryMinutes());
    Instant expiresAt = Instant.now().plus(previewTtl);

    List<DocumentPreviewResponse> previews =
        documents.stream()
            .map(
                doc -> {
                  String previewUrl =
                      storageService.generatePreviewUrl(doc.getFileStorageUrl(), previewTtl);
                  return documentMapper.toPreviewResponse(doc, previewUrl, expiresAt);
                })
            .toList();

    log.debug(
        "Generated {} preview URLs for organization: {}, expiresAt={}",
        previews.size(),
        organizationId,
        expiresAt);

    return previews;
  }

  public Set<DocumentType> getRequiredDocuments(BusinessRegistrationType registrationType) {
    return registrationType.requiredDocuments();
  }

  public boolean hasAllRequiredDocumentsActive(
      UUID organizationId, BusinessRegistrationType registrationType) {
    Set<DocumentType> required = getRequiredDocuments(registrationType);
    if (required.isEmpty()) {
      return true;
    }
    Set<DocumentType> active =
        documentRepository
            .findByOrganizationIdAndStatus(organizationId, DocumentStatus.ACTIVE)
            .stream()
            .map(Document::getDocumentType)
            .collect(Collectors.toSet());
    return active.containsAll(required);
  }

  @Transactional
  public DocumentUploadInitiationResponse initiateUpload(
      DocumentType documentType, String fileName, String contentType, LocalDate expiryDate) {
    log.info("Initiating document upload: documentType={}, fileName={}", documentType, fileName);

    validateContentType(documentType, contentType);
    LocalDate resolvedExpiryDate = resolveExpiryDate(documentType, expiryDate);

    long maxFileSizeBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;
    String sanitizedFileName = storageService.sanitizeFilename(fileName);

    Document document =
        Document.builder()
            .documentType(documentType)
            .expiryDate(resolvedExpiryDate)
            .fileName(sanitizedFileName)
            .mimeType(contentType)
            .build();

    document = documentRepository.save(document);

    String key = generateDocumentKey(documentType, document.getId(), sanitizedFileName);
    document.assignStorageKey(key);
    Document saved = documentRepository.save(document);

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            contentType,
            maxFileSizeBytes,
            Map.of("documentId", saved.getId().toString(), DOCUMENT_TYPE, documentType.name()));

    log.info("Upload initiated: documentId={}, key={}", saved.getId(), key);

    return documentMapper.toUploadInitiationResponse(document.getId(), key, uploadData);
  }

  @Transactional
  public DocumentUploadInitiationResponse replaceDocument(
      UUID documentId, String fileName, String contentType, LocalDate expiryDate) {
    log.info("Initiating document replacement: documentId={}, fileName={}", documentId, fileName);

    Document oldDocument = requireById(documentId);
    validateDocumentStatus(
        oldDocument, List.of(DocumentStatus.ACTIVE, DocumentStatus.EXPIRED), "replace");
    validateContentType(oldDocument.getDocumentType(), contentType);

    LocalDate resolvedExpiryDate = resolveExpiryDate(oldDocument.getDocumentType(), expiryDate);
    long maxFileSizeBytes = awsProperties.getS3().getMaxFileSizeMb() * BYTES_PER_MEGABYTE;
    String sanitizedFileName = storageService.sanitizeFilename(fileName);

    Document newDocument =
        Document.builder()
            .documentType(oldDocument.getDocumentType())
            .expiryDate(resolvedExpiryDate)
            .fileName(sanitizedFileName)
            .mimeType(contentType)
            .build();

    newDocument = documentRepository.save(newDocument);

    String key =
        generateDocumentKey(oldDocument.getDocumentType(), newDocument.getId(), sanitizedFileName);

    newDocument.assignStorageKey(key);
    newDocument.designateAsReplacementFor(oldDocument);
    oldDocument.markReplaced();

    documentRepository.save(newDocument);
    documentRepository.save(oldDocument);

    makerCheckerTemplate.publish(
        newDocument,
        oldDocument.getOrganization().getId().toString(),
        "REPLACE_DOCUMENT",
        List.of(),
        Map.of(DOCUMENT_TYPE, oldDocument.getDocumentType(), "oldDocumentId", oldDocument.getId()));

    UploadUrlData uploadData =
        storageService.generateUploadUrl(
            key,
            contentType,
            maxFileSizeBytes,
            Map.of(
                "documentId",
                newDocument.getId().toString(),
                DOCUMENT_TYPE,
                oldDocument.getDocumentType().name(),
                "organizationId",
                oldDocument.getOrganization().getId().toString(),
                "replacesDocumentId",
                oldDocument.getId().toString()));

    log.info(
        "Replacement initiated: oldDocumentId={}, newDocumentId={}, key={}",
        documentId,
        newDocument.getId(),
        key);

    return new DocumentUploadInitiationResponse(
        newDocument.getId(), uploadData.expiryTime(), key, uploadData.headers(), uploadData.url());
  }

  @Transactional(readOnly = true)
  public void validateRequiredDocumentsUploaded(
      UUID organizationId, BusinessRegistrationType registrationType) {
    log.info(
        "Validating required documents: organizationId={}, registrationType={}",
        organizationId,
        registrationType);

    Set<DocumentType> required = registrationType.requiredDocuments();

    Set<DocumentType> uploaded =
        documentRepository
            .findByOrganizationIdAndStatus(organizationId, DocumentStatus.UPLOADED)
            .stream()
            .map(Document::getDocumentType)
            .collect(Collectors.toSet());

    Set<DocumentType> missing = new HashSet<>(required);
    missing.removeAll(uploaded);

    if (!missing.isEmpty()) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          String.format(
              "Missing required documents for organization %s: %s", organizationId, missing),
          "documents",
          missing.toArray());
    }

    log.debug("All required documents validated for organization: {}", organizationId);
  }

  Document requireById(UUID documentId) {
    return documentRepository
        .findById(documentId)
        .orElseThrow(
            () ->
                new ResourceNotFoundException(
                    ResponseCode.RESOURCE_NOT_FOUND, "Document not found with ID: " + documentId));
  }

  private String generateDocumentKey(DocumentType documentType, UUID documentId, String fileName) {
    return String.format(
        "documents/%s/%s-%s-%s", documentType, documentId, System.currentTimeMillis(), fileName);
  }

  private LocalDate resolveExpiryDate(DocumentType documentType, LocalDate providedExpiryDate) {
    if (documentType.hasConventionalExpiry()) {
      return LocalDate.now(ZoneOffset.UTC).plus(documentType.conventionalExpiryPeriod());
    }
    if (documentType.requiresExpiryDate()) {
      if (providedExpiryDate == null) {
        throw new ValidationException(
            ResponseCode.INVALID_ARGUMENTS,
            "Expiry date is required for document type: " + documentType,
            DOCUMENT_TYPE,
            documentType);
      }
      return providedExpiryDate;
    }
    return null;
  }

  private void validateContentType(DocumentType documentType, String contentType) {
    Map<DocumentType, String> allowedTypesConfig = awsProperties.getS3().getAllowedContentTypes();
    String allowedContentTypes = allowedTypesConfig.get(documentType);

    if (allowedContentTypes == null) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          "No content types configured for " + documentType,
          DOCUMENT_TYPE,
          documentType);
    }

    boolean isAllowed =
        Stream.of(allowedContentTypes.split(","))
            .map(String::trim)
            .anyMatch(allowed -> allowed.equalsIgnoreCase(contentType));

    if (!isAllowed) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format(
              "Content type %s not allowed for %s. Allowed types: %s",
              contentType, documentType, allowedContentTypes),
          "templateContentType",
          contentType);
    }
  }

  private void validateDocumentStatus(
      Document document, List<DocumentStatus> expectedStatuses, String operation) {
    if (!expectedStatuses.contains(document.getStatus())) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          String.format(
              "Document is in invalid state for %s: expected one of %s, found %s",
              operation, expectedStatuses, document.getStatus()));
    }
  }

  private void validateIsReplacementDocument(Document document) {
    if (document.getReplacesDocument() == null) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Document is not a replacement document");
    }
  }

  private void validateStoredFile(String key, String expectedMimeType) {
    StoredFileMetadata metadata = storageService.getStoredFileMetadata(key);

    if (!metadata.exists()) {
      throw new ResourceNotFoundException(
          ResponseCode.RESOURCE_NOT_FOUND, "File not found in storage: " + key);
    }

    if (!metadata.contentType().equals(expectedMimeType)) {
      throw new ValidationException(
          ResponseCode.INVALID_ARGUMENTS,
          String.format(
              "Content type mismatch: expected %s, found %s",
              expectedMimeType, metadata.contentType()),
          "mimeType",
          expectedMimeType);
    }
  }

  private void validateUploadConfirmation(
      Document document, DocumentUploadConfirmationRequest request) {
    validateDocumentStatus(document, List.of(DocumentStatus.PENDING), "confirm upload");
    validateStoredFile(document.getFileStorageUrl(), request.mimeType());
  }
}
