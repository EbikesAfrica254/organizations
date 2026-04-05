package com.ebikes.organizations.support.context;

import java.util.Collections;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class ExecutionContext {

  private static final ThreadLocal<Context> context = new ThreadLocal<>();

  private ExecutionContext() {}

  public sealed interface Context permits UserContext, SystemContext {}

  public record UserContext(
      String userId,
      String activeOrganization,
      String activeBranch,
      String email,
      String phoneNumber,
      Set<String> groups,
      Set<String> roles)
      implements Context {

    public UserContext {
      groups = groups != null ? Set.copyOf(groups) : Collections.emptySet();
      roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
    }

    @Override
    public Set<String> groups() {
      return Set.copyOf(groups);
    }

    @Override
    public Set<String> roles() {
      return Set.copyOf(roles);
    }
  }

  public record SystemContext() implements Context {}

  public static Context get() {
    Context ctx = context.get();
    if (ctx == null) {
      throw new IllegalStateException("No execution context on current thread");
    }
    return ctx;
  }

  public static String getUserId() {
    return switch (get()) {
      case UserContext uc -> uc.userId();
      case SystemContext ignored ->
          throw new IllegalStateException(
              "getUserId() called in system context — use get() and pattern match");
    };
  }

  public static void set(
      String userId,
      String activeOrganization,
      String activeBranch,
      String email,
      Set<String> groups,
      String phoneNumber,
      Set<String> roles) {
    if (userId == null || userId.isBlank()) {
      log.warn("Attempted to set null or blank userId in ExecutionContext");
      throw new IllegalArgumentException("userId cannot be null or blank");
    }
    context.set(
        new UserContext(
            userId, activeOrganization, activeBranch, email, phoneNumber, groups, roles));
  }

  public static void setSystem() {
    context.set(new SystemContext());
  }

  public static void clear() {
    context.remove();
  }
}
