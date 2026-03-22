package com.ebikes.organizations.support.changes;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ebikes.organizations.constants.EventConstants.EventSource;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.events.outgoing.MakerCheckerRequest;
import com.ebikes.organizations.dtos.internal.FieldChange;

import lombok.experimental.UtilityClass;

@UtilityClass
public class MakerCheckerRequestBuilder {

  private static final String ENTITY_TYPE_DOCUMENT = "DOCUMENT";
  private static final String ENTITY_TYPE_ORGANIZATION = "ORGANIZATION";
  private static final String OPERATION = "operation";

  public static MakerCheckerRequest forDocumentReplacement(
      Document newDocument, Document oldDocument, String makerId) {
    return new MakerCheckerRequest(
        null,
        List.of(),
        newDocument.getId(),
        ENTITY_TYPE_DOCUMENT,
        makerId,
        Map.of(
            "documentType",
            oldDocument.getDocumentType(),
            "oldDocumentId",
            oldDocument.getId(),
            OPERATION,
            "REPLACE_DOCUMENT"),
        oldDocument.getOrganization().getId().toString(),
        null,
        EventSource.serviceReference());
  }

  public static MakerCheckerRequest forOrganizationCreate(
      Organization organization, List<FieldChange> changes, String makerId) {
    return new MakerCheckerRequest(
        null,
        changes,
        organization.getId(),
        ENTITY_TYPE_ORGANIZATION,
        makerId,
        Map.of(OPERATION, "CREATE"),
        organization.getId().toString(),
        null,
        EventSource.serviceReference());
  }

  public static MakerCheckerRequest forOrganizationUpdate(
      UUID organizationId, List<FieldChange> changes, String makerId) {
    return new MakerCheckerRequest(
        null,
        changes,
        organizationId,
        ENTITY_TYPE_ORGANIZATION,
        makerId,
        Map.of(OPERATION, "UPDATE"),
        organizationId.toString(),
        null,
        EventSource.serviceReference());
  }
}
