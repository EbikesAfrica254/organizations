package com.ebikes.organizations.enums;

import java.util.Set;

public enum BusinessRegistrationType {
  INDIVIDUAL(
      Set.of(
          DocumentType.KRA_PIN_CERTIFICATE,
          DocumentType.NATIONAL_ID_FRONT,
          DocumentType.NATIONAL_ID_BACK)),

  SOLE_PROPRIETOR(
      Set.of(
          DocumentType.KRA_PIN_CERTIFICATE,
          DocumentType.NATIONAL_ID_FRONT,
          DocumentType.NATIONAL_ID_BACK,
          DocumentType.BUSINESS_NAME_CERTIFICATE)),

  PARTNERSHIP(
      Set.of(
          DocumentType.KRA_PIN_CERTIFICATE,
          DocumentType.NATIONAL_ID_FRONT,
          DocumentType.NATIONAL_ID_BACK,
          DocumentType.PARTNERSHIP_DEED,
          DocumentType.BUSINESS_NAME_CERTIFICATE)),

  PRIVATE_LIMITED_COMPANY(
      Set.of(
          DocumentType.KRA_PIN_CERTIFICATE,
          DocumentType.CERTIFICATE_OF_INCORPORATION,
          DocumentType.MEMORANDUM_AND_ARTICLES,
          DocumentType.CR12)),

  FOREIGN_ENTITY(
      Set.of(
          DocumentType.KRA_PIN_CERTIFICATE,
          DocumentType.PASSPORT,
          DocumentType.CERTIFICATE_OF_INCORPORATION,
          DocumentType.FOREIGN_COMPANY_CERTIFICATE,
          DocumentType.CR12));

  private final Set<DocumentType> requiredDocuments;

  BusinessRegistrationType(Set<DocumentType> requiredDocuments) {
    this.requiredDocuments = requiredDocuments;
  }

  public Set<DocumentType> requiredDocuments() {
    return Set.copyOf(requiredDocuments);
  }
}
