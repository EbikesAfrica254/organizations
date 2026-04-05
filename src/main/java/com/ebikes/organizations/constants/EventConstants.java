package com.ebikes.organizations.constants;

import com.ebikes.organizations.support.references.ReferenceGenerator;

public final class EventConstants {

  private EventConstants() {}

  public static final class Source {

    private Source() {}

    public static final String ORGANIZATIONS = "organizations";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(ORGANIZATIONS);
    }
  }

  public static final class DomainEvents {

    private DomainEvents() {}

    public static final class Branch {

      private Branch() {}

      public static final String CREATED = Source.ORGANIZATIONS + ".branch.created";
      public static final String DEACTIVATED = Source.ORGANIZATIONS + ".branch.deactivated";
      public static final String REINSTATED = Source.ORGANIZATIONS + ".branch.reinstated";
      public static final String SUSPENDED = Source.ORGANIZATIONS + ".branch.suspended";
      public static final String UPDATED = Source.ORGANIZATIONS + ".branch.updated";
    }

    public static final class Document {

      private Document() {}

      public static final String ARCHIVED = Source.ORGANIZATIONS + ".document.archived";
      public static final String EXPIRED = Source.ORGANIZATIONS + ".document.expired";
      public static final String UPLOADED = Source.ORGANIZATIONS + ".document.uploaded";
    }

    public static final class Organization {

      private Organization() {}

      public static final String APPROVED = Source.ORGANIZATIONS + ".organization.approved";
      public static final String CREATED = Source.ORGANIZATIONS + ".organization.created";
      public static final String COMPLIANCE_UPDATED =
          Source.ORGANIZATIONS + ".organization.compliance-updated";
      public static final String DEACTIVATED = Source.ORGANIZATIONS + ".organization.deactivated";
      public static final String REJECTED = Source.ORGANIZATIONS + ".organization.rejected";
      public static final String UPDATED = Source.ORGANIZATIONS + ".organization.updated";
    }
  }

  public static final class ExternalContracts {

    private ExternalContracts() {}

    // inbound — maker-checker decisions arrive as maker-checker.organization.<outcome>
    public static final String MAKER_CHECKER_ORGANIZATION = "maker-checker.organization";
  }

  public static final class MessageHeaders {

    private MessageHeaders() {}

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }
}
