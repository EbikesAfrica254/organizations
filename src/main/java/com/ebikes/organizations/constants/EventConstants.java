package com.ebikes.organizations.constants;

import com.ebikes.organizations.support.references.ReferenceGenerator;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EventConstants {

  public static final class EventTypes {

    private EventTypes() {
      throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
    }

    public static final class Branches {

      private Branches() {
        throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
      }

      public static final String CREATED = EventSource.HOST_SERVICE + ".branch.created";
      public static final String DEACTIVATED = EventSource.HOST_SERVICE + ".branch.deactivated";
      public static final String REINSTATED = EventSource.HOST_SERVICE + ".branch.reinstated";
      public static final String SUSPENDED = EventSource.HOST_SERVICE + ".branch.suspended";
      public static final String UPDATED = EventSource.HOST_SERVICE + ".branch.updated";
    }

    public static final class Documents {

      private Documents() {
        throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
      }

      public static final String UPLOADED = EventSource.HOST_SERVICE + ".document.uploaded";
    }

    public static final class Organizations {

      private Organizations() {
        throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
      }

      public static final String APPROVED = EventSource.HOST_SERVICE + ".organization.approved";
      public static final String DEACTIVATED =
          EventSource.HOST_SERVICE + ".organization.deactivated";
      public static final String REJECTED = EventSource.HOST_SERVICE + ".organization.rejected";
    }
  }

  public static final class EventSource {

    private EventSource() {
      throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
    }

    public static final String HOST_SERVICE = "organizations";

    public static String serviceReference() {
      return ReferenceGenerator.generateServiceReference(HOST_SERVICE);
    }
  }

  public static final class MessageHeaders {

    private MessageHeaders() {
      throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
    }

    public static final String EVENT_TYPE = "eventType";
    public static final String OUTBOX_ID = "outboxId";
    public static final String ROUTING_KEY = "routingKey";
  }

  public static final class RoutingKeys {

    private RoutingKeys() {
      throw new UnsupportedOperationException(ApplicationConstants.CLASS_CANNOT_BE_INSTANTIATED);
    }

    // outbound — audit routing keys: <service>.<entity>.audit → matches *.*.audit binding
    public static final String ORGANIZATIONS_BRANCH_AUDIT =
        audit(EventSource.HOST_SERVICE + ".branch");
    public static final String ORGANIZATIONS_DOCUMENT_AUDIT =
        audit(EventSource.HOST_SERVICE + ".document");
    public static final String ORGANIZATIONS_ORGANIZATION_AUDIT =
        audit(EventSource.HOST_SERVICE + ".organization");

    // outbound — maker-checker request routing keys: <service>.<entity>.maker-checker-request
    //            → matches *.*.maker-checker-request binding
    public static final String ORGANIZATIONS_DOCUMENT_MAKER_CHECKER_REQUEST =
        makerCheckerRequest(EventSource.HOST_SERVICE, "document");
    public static final String ORGANIZATIONS_ORGANIZATION_MAKER_CHECKER_REQUEST =
        makerCheckerRequest(EventSource.HOST_SERVICE, "organization");

    // inbound — maker-checker decision prefix: matches maker-checker.organization.# binding
    //           decisions arrive as maker-checker.organization.<outcome>
    public static final String MAKER_CHECKER_ORGANIZATION = "maker-checker.organization";

    public static String audit(String domain) {
      return domain + ".audit";
    }

    public static String makerCheckerRequest(String sourceService, String entityType) {
      return sourceService
          + "."
          + entityType.toLowerCase().replace("_", "-")
          + ".maker-checker-request";
    }
  }
}
