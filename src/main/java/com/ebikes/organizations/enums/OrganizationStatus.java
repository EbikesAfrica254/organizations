package com.ebikes.organizations.enums;

public enum OrganizationStatus {
  ACTIVE, // Operational, can accept orders
  APPROVED, // Approval service approved, awaiting activation
  DEACTIVATED,
  PENDING_APPROVAL, // Created, waiting for approval service decision
  REJECTED // Approval service rejected, can resubmit
}
