package com.ebikes.organizations.enums;

import java.time.Period;

public enum DocumentType {
  BUSINESS_NAME_CERTIFICATE(true, true, false, null),
  CERTIFICATE_OF_COMPLIANCE(true, true, false, null),
  CERTIFICATE_OF_INCORPORATION(false, false, false, null),
  CR12(true, false, true, Period.ofMonths(12)),
  FOREIGN_COMPANY_CERTIFICATE(false, false, false, null),
  KRA_PIN_CERTIFICATE(false, false, false, null),
  MEMORANDUM_AND_ARTICLES(false, false, false, null),
  NATIONAL_ID_BACK(false, false, false, null),
  NATIONAL_ID_FRONT(false, false, false, null),
  PARTNERSHIP_DEED(false, false, false, null),
  PASSPORT(true, true, false, null),
  PUBLIC_HEALTH_CERTIFICATE(true, true, false, null),
  SINGLE_BUSINESS_PERMIT(true, true, false, null);

  private final Period conventionalExpiryPeriod;
  private final boolean expires;
  private final boolean hasConventionalExpiry;
  private final boolean requiresExpiryDate;

  DocumentType(
      boolean expires,
      boolean requiresExpiryDate,
      boolean hasConventionalExpiry,
      Period conventionalExpiryPeriod) {
    if (!expires && (requiresExpiryDate || hasConventionalExpiry)) {
      throw new IllegalArgumentException(
          name() + ": requiresExpiryDate and hasConventionalExpiry require expires=true");
    }
    if (requiresExpiryDate && hasConventionalExpiry) {
      throw new IllegalArgumentException(
          name() + ": requiresExpiryDate and hasConventionalExpiry are mutually exclusive");
    }
    if (hasConventionalExpiry && conventionalExpiryPeriod == null) {
      throw new IllegalArgumentException(
          name() + ": conventionalExpiryPeriod is required when hasConventionalExpiry=true");
    }
    this.expires = expires;
    this.requiresExpiryDate = requiresExpiryDate;
    this.hasConventionalExpiry = hasConventionalExpiry;
    this.conventionalExpiryPeriod = conventionalExpiryPeriod;
  }

  public boolean expires() {
    return expires;
  }

  public boolean requiresExpiryDate() {
    return requiresExpiryDate;
  }

  public boolean hasConventionalExpiry() {
    return hasConventionalExpiry;
  }

  public Period conventionalExpiryPeriod() {
    return conventionalExpiryPeriod;
  }
}
