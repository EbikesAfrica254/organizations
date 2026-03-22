package com.ebikes.organizations.constants;

public final class MDCKeys {
  public static final String ACTIVE_BRANCH = "activeBranch";
  public static final String ACTIVE_ORGANIZATION = "activeOrganization";
  public static final String ERROR_REFERENCE = "errorReference";
  public static final String IP_ADDRESS = "ipAddress";
  public static final String REQUEST_ID = "requestId";
  public static final String REQUEST_PATH = "requestPath";
  public static final String USER_ID = "userId";

  private MDCKeys() {
    throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
  }
}
