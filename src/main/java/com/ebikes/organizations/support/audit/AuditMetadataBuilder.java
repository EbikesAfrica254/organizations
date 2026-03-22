package com.ebikes.organizations.support.audit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;

import lombok.experimental.UtilityClass;

@UtilityClass
public class AuditMetadataBuilder {

  private static final String BRANCH_NAME       = "branchName";
  private static final String COMPLIANCE_STATUS = "complianceStatus";
  private static final String DISPLAY_NAME      = "displayName";
  private static final String DOCUMENT_TYPE     = "documentType";
  private static final String ORGANIZATION_ID   = "organizationId";
  private static final String OWNER_ID          = "ownerId";
  private static final String REGISTRATION_TYPE = "registrationType";
  private static final String STATUS            = "status";

  public static Map<String, String> forBranch(Branch branch) {
    return Map.of(
            BRANCH_NAME,     branch.getBranchName(),
            DISPLAY_NAME,    branch.getDisplayName(),
            ORGANIZATION_ID, branch.getOrganization().getId().toString(),
            STATUS,          branch.getStatus().name());
  }

  public static Map<String, String> forBranch(Branch branch, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forBranch(branch));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }

  public static Map<String, String> forDocument(Document document) {
    return Map.of(
            DOCUMENT_TYPE,   document.getDocumentType().name(),
            ORGANIZATION_ID, document.getOrganization() != null
                    ? document.getOrganization().getId().toString()
                    : "",
            STATUS,          document.getStatus().name());
  }

  public static Map<String, String> forOrganization(Organization organization) {
    return Map.of(
            COMPLIANCE_STATUS, organization.getComplianceStatus().name(),
            DISPLAY_NAME,      organization.getDisplayName(),
            OWNER_ID,          organization.getOwnerId(),
            REGISTRATION_TYPE, organization.getRegistrationType().name(),
            STATUS,            organization.getStatus().name());
  }

  public static Map<String, String> forOrganization(
          Organization organization, Map<String, String> extra) {
    Map<String, String> metadata = new HashMap<>(forOrganization(organization));
    metadata.putAll(extra);
    return Collections.unmodifiableMap(metadata);
  }
}