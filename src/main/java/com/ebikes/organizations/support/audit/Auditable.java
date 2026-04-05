package com.ebikes.organizations.support.audit;

import java.util.Map;

public interface Auditable {
  Map<String, String> toAuditMetadata();
}
