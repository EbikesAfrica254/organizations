package com.ebikes.organizations.support.fixtures;

import java.math.BigDecimal;
import java.util.List;

import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DeactivateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DocumentUploadInfo;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.DocumentType;

import net.datafaker.Faker;

public final class OrganizationRequestFixtures {

  private static final Faker FAKER = new Faker();

  private OrganizationRequestFixtures() {}

  public static CreateOrganizationRequest create() {
    return create(List.of(defaultAddress()), List.of(defaultDocument()));
  }

  public static CreateOrganizationRequest create(
      List<Address> addresses, List<DocumentUploadInfo> documents) {
    return new CreateOrganizationRequest(
        addresses,
        documents,
        FAKER.company().name(),
        FAKER.internet().emailAddress(),
        null,
        null,
        FAKER.company().name(),
        SecurityFixtures.TEST_USER_ID,
        SecurityFixtures.TEST_PHONE_NUMBER,
        null,
        BusinessRegistrationType.PRIVATE_LIMITED_COMPANY);
  }

  public static DeactivateOrganizationRequest deactivate() {
    return new DeactivateOrganizationRequest("Compliance issue");
  }

  public static UpdateOrganizationRequest update() {
    return update(FAKER.company().name());
  }

  public static UpdateOrganizationRequest update(String legalName) {
    return new UpdateOrganizationRequest(
        null, null, null, null, null, null, legalName, null, null, null);
  }

  private static Address defaultAddress() {
    return new Address(
        AddressTag.PRIMARY,
        FAKER.address().city(),
        FAKER.address().country(),
        BigDecimal.valueOf(FAKER.number().randomDouble(6, -90, 90)),
        BigDecimal.valueOf(FAKER.number().randomDouble(6, -180, 180)),
        FAKER.address().zipCode(),
        FAKER.address().streetAddress());
  }

  private static DocumentUploadInfo defaultDocument() {
    String key = "documents/KRA_PIN_CERT/" + FAKER.file().fileName();
    return new DocumentUploadInfo(
        DocumentType.KRA_PIN_CERTIFICATE,
        null,
        FAKER.file().fileName(),
        FAKER.number().numberBetween(1024L, 10485760L),
        "application/pdf",
        key);
  }
}
