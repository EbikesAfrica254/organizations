package com.ebikes.organizations.support.fixtures;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.ebikes.organizations.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentUploadInitiationResponse;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;

public final class DocumentResponseFixtures {

  private static final UUID DOCUMENT_ID = UUID.randomUUID();
  private static final String FILE_NAME = "test-file.pdf";
  private static final String MIME_TYPE = "application/pdf";
  private static final String PREVIEW_URL = "https://preview.url";
  private static final String UPLOAD_URL = "https://upload.url";

  private DocumentResponseFixtures() {}

  public static DocumentPreviewResponse previewResponse(UUID organizationId) {
    return new DocumentPreviewResponse(
        DocumentType.BUSINESS_NAME_CERTIFICATE,
        null,
        FILE_NAME,
        DOCUMENT_ID,
        MIME_TYPE,
        organizationId,
        PREVIEW_URL,
        Instant.now(),
        DocumentStatus.ACTIVE,
        null);
  }

  public static DocumentResponse response(UUID organizationId) {
    return new DocumentResponse(
        null,
        DocumentType.BUSINESS_NAME_CERTIFICATE,
        null,
        FILE_NAME,
        1024L,
        DOCUMENT_ID,
        MIME_TYPE,
        organizationId,
        null,
        DocumentStatus.UPLOADED,
        null,
        null);
  }

  public static DocumentSummaryResponse summaryResponse(UUID organizationId) {
    return new DocumentSummaryResponse(
        DocumentType.BUSINESS_NAME_CERTIFICATE,
        null,
        FILE_NAME,
        DOCUMENT_ID,
        organizationId,
        DocumentStatus.ACTIVE,
        null);
  }

  public static DocumentUploadInitiationResponse uploadInitiationResponse() {
    return new DocumentUploadInitiationResponse(
        DOCUMENT_ID,
        Instant.now(),
        "documents/KRA_PIN_CERT/" + DOCUMENT_ID + "-test-file.pdf",
        Map.of(),
        UPLOAD_URL);
  }
}
