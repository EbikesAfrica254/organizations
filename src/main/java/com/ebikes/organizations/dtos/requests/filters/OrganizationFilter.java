package com.ebikes.organizations.dtos.requests.filters;

import java.time.OffsetDateTime;

import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrganizationFilter extends BaseFilter {

  private OffsetDateTime activatedAtFrom;
  private OffsetDateTime activatedAtTo;
  private ComplianceStatus complianceStatus;
  private OffsetDateTime createdAtFrom;
  private OffsetDateTime createdAtTo;
  private String legalName;
  private BusinessRegistrationType registrationType;
  private OrganizationStatus status;
}
