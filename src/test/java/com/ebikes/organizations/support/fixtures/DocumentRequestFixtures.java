package com.ebikes.organizations.support.fixtures;

import com.ebikes.organizations.dtos.requests.documents.DocumentUploadConfirmationRequest;
import com.ebikes.organizations.dtos.requests.documents.DocumentUploadInitiationRequest;
import com.ebikes.organizations.enums.DocumentType;

public final class DocumentRequestFixtures {

  private static final String FILE_NAME = "test-file.pdf";
  private static final String MIME_TYPE = "application/pdf";

  private DocumentRequestFixtures() {}

  public static DocumentUploadConfirmationRequest confirmationRequest() {
    return new DocumentUploadConfirmationRequest(1024L, MIME_TYPE);
  }

  public static DocumentUploadInitiationRequest initiationRequest() {
    return new DocumentUploadInitiationRequest(
        MIME_TYPE, DocumentType.KRA_PIN_CERTIFICATE, null, FILE_NAME);
  }
}
