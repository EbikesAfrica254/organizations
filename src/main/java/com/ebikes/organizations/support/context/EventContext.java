package com.ebikes.organizations.support.context;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class EventContext {

  private static final ThreadLocal<EventData> context = new ThreadLocal<>();

  private EventContext() {
    // prevent instantiation
  }

  public static boolean absent() {
    return context.get() == null;
  }

  public static void clear() {
    context.remove();
  }

  public static String getEventType() {
    EventData data = context.get();
    return data != null ? data.eventType() : null;
  }

  public static String getRoutingKey() {
    EventData data = context.get();
    return data != null ? data.assignmentsKey() : null;
  }

  public static String getSourceService() {
    EventData data = context.get();
    return data != null ? data.sourceService() : null;
  }

  public static void set(
      String correlationId, String eventType, String assignmentsKey, String sourceService) {
    context.set(new EventData(correlationId, eventType, assignmentsKey, sourceService));
  }

  public record EventData(
      String correlationId, String eventType, String assignmentsKey, String sourceService) {}
}
