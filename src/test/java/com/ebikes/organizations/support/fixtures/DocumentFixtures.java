package com.ebikes.organizations.support.fixtures;

import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.enums.DocumentType;

public final class DocumentFixtures {

  private DocumentFixtures() {}

  public static Document active(Organization organization) {
    Document document = uploaded(organization);
    document.activate();
    return document;
  }

  public static Document active(Organization organization, DocumentType documentType) {
    Document document = uploaded(organization, documentType);
    document.activate();
    return document;
  }

  public static Document pending(Organization organization) {
    return base(organization, DocumentType.KRA_PIN_CERTIFICATE).build();
  }

  public static Document pending(Organization organization, DocumentType documentType) {
    return base(organization, documentType).build();
  }

  public static Document replaced(Organization organization) {
    Document document = active(organization);
    document.markReplaced();
    return document;
  }

  public static Document uploaded(Organization organization) {
    Document document = pending(organization);
    document.markUploaded(1024L, "application/pdf");
    return document;
  }

  public static Document uploaded(Organization organization, DocumentType documentType) {
    Document document = pending(organization, documentType);
    document.markUploaded(1024L, "application/pdf");
    return document;
  }

  private static Document.DocumentBuilder<?, ?> base(
      Organization organization, DocumentType documentType) {
    return Document.builder().documentType(documentType).organization(organization);
  }
}
