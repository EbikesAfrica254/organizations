package com.ebikes.organizations.configurations.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.constants.ApplicationConstants;
import com.ebikes.organizations.constants.MDCKeys;
import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.support.context.ExecutionContext;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.references.ReferenceGenerator;

@DisplayName("ContextFilter")
class ContextFilterTest {

  private static final String ACTIVE_BRANCH = "branch-1";
  private static final String ACTIVE_ORGANIZATION = SecurityFixtures.TEST_ORGANIZATION_ID;
  private static final String GENERATED_REQUEST_ID = "ERR-123456";
  private static final String REQUEST_ID = "REQ-123456";
  private static final String USER_ID = SecurityFixtures.TEST_USER_ID;

  private final ContextFilter contextFilter = new ContextFilter();

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    ExecutionContext.clear();
    MDC.clear();
  }

  @Test
  @DisplayName("should populate execution context and MDC for JWT authentication")
  void shouldPopulateExecutionContextAndMdcForJwtAuthentication() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            SecurityFixtures.authentication(
                USER_ID,
                ACTIVE_ORGANIZATION,
                ACTIVE_BRANCH,
                List.of("/org/admins", "/ops"),
                Map.of(
                    "roles",
                    List.of(UserRole.ORGANIZATION_ADMIN.name(), UserRole.CUSTOMER.name()))));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/contacts");
    request.addHeader(ApplicationConstants.REQUEST_ID_HEADER, REQUEST_ID);
    request.addHeader("X-Forwarded-For", "203.0.113.10, 10.0.0.1");

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        chainAsserts(
            () -> {
              assertThat(currentContextData()).isInstanceOf(ExecutionContext.UserContext.class);
              ExecutionContext.UserContext ctx =
                  (ExecutionContext.UserContext) currentContextData();

              assertThat(ctx.userId()).isEqualTo(USER_ID);
              assertThat(ctx.activeOrganization()).isEqualTo(ACTIVE_ORGANIZATION);
              assertThat(ctx.activeBranch()).isEqualTo(ACTIVE_BRANCH);
              assertThat(ctx.email()).isEqualTo(SecurityFixtures.TEST_EMAIL);
              assertThat(ctx.phoneNumber()).isEqualTo(SecurityFixtures.TEST_PHONE_NUMBER);
              assertThat(ctx.roles())
                  .containsExactlyInAnyOrder(
                      UserRole.ORGANIZATION_ADMIN.name(), UserRole.CUSTOMER.name());
              assertThat(ctx.groups()).containsExactlyInAnyOrder("/org/admins", "/ops");

              assertThat(MDC.get(MDCKeys.USER_ID)).isEqualTo(USER_ID);
              assertThat(MDC.get(MDCKeys.IP_ADDRESS)).isEqualTo("203.0.113.10");
              assertThat(MDC.get(MDCKeys.REQUEST_PATH)).isEqualTo("/api/contacts");
              assertThat(MDC.get(MDCKeys.ACTIVE_ORGANIZATION)).isEqualTo(ACTIVE_ORGANIZATION);
              assertThat(MDC.get(MDCKeys.ACTIVE_BRANCH)).isEqualTo(ACTIVE_BRANCH);
              assertThat(MDC.get(MDCKeys.REQUEST_ID)).isEqualTo(REQUEST_ID);
            });

    contextFilter.doFilter(request, response, filterChain);

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  @DisplayName("should use generated request id when request header is missing")
  void shouldUseGeneratedRequestIdWhenRequestHeaderIsMissing() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            SecurityFixtures.authentication(
                USER_ID,
                ACTIVE_ORGANIZATION,
                null,
                List.of(),
                Map.of("roles", List.of(UserRole.ORGANIZATION_ADMIN.name()))));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/users/me");
    request.addHeader("X-Real-IP", "198.51.100.15");

    MockHttpServletResponse response = new MockHttpServletResponse();

    try (var mockedReferenceGenerator = mockStatic(ReferenceGenerator.class)) {
      mockedReferenceGenerator
          .when(ReferenceGenerator::generateErrorReference)
          .thenReturn(GENERATED_REQUEST_ID);

      FilterChain filterChain =
          chainAsserts(
              () -> {
                assertThat(MDC.get(MDCKeys.REQUEST_ID)).isEqualTo(GENERATED_REQUEST_ID);
                assertThat(MDC.get(MDCKeys.IP_ADDRESS)).isEqualTo("198.51.100.15");
              });

      contextFilter.doFilter(request, response, filterChain);
    }

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  @DisplayName("should set system context when JWT is missing sub claim")
  void shouldSetSystemContextWhenJwtIsMissingSubClaim() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authenticationWithoutSub());

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/context");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain =
        chainAsserts(
            () ->
                assertThat(currentContextData())
                    .isInstanceOf(ExecutionContext.SystemContext.class));

    contextFilter.doFilter(request, response, filterChain);

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  @DisplayName("should set system context when authentication is not JWT")
  void shouldSetSystemContextWhenAuthenticationIsNotJwt() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new TestingAuthenticationToken("principal", "credentials"));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/context");
    MockHttpServletResponse response = new MockHttpServletResponse();

    FilterChain filterChain =
        chainAsserts(
            () ->
                assertThat(currentContextData())
                    .isInstanceOf(ExecutionContext.SystemContext.class));

    contextFilter.doFilter(request, response, filterChain);

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  @DisplayName("should return empty roles when realm access roles are malformed")
  void shouldReturnEmptyRolesWhenRealmAccessRolesAreMalformed() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            SecurityFixtures.authentication(
                USER_ID,
                ACTIVE_ORGANIZATION,
                null,
                List.of("group-a"),
                Map.of("roles", "not-a-list")));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/users/me");
    request.setRemoteAddr("127.0.0.1");

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        chainAsserts(
            () -> {
              assertThat(currentContextData()).isInstanceOf(ExecutionContext.UserContext.class);
              ExecutionContext.UserContext ctx =
                  (ExecutionContext.UserContext) currentContextData();

              assertThat(ctx.userId()).isEqualTo(USER_ID);
              assertThat(ctx.roles()).isEmpty();
              assertThat(ctx.groups()).containsExactly("group-a");
              assertThat(MDC.get(MDCKeys.IP_ADDRESS)).isEqualTo("127.0.0.1");
            });

    contextFilter.doFilter(request, response, filterChain);

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  @Test
  @DisplayName("should clear execution context and MDC when filter chain throws")
  void shouldClearExecutionContextAndMdcWhenFilterChainThrows() {
    SecurityContextHolder.getContext()
        .setAuthentication(
            SecurityFixtures.authentication(
                USER_ID,
                ACTIVE_ORGANIZATION,
                ACTIVE_BRANCH,
                List.of(),
                Map.of("roles", List.of(UserRole.ORGANIZATION_ADMIN.name()))));

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/failure");
    request.addHeader(ApplicationConstants.REQUEST_ID_HEADER, REQUEST_ID);

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain =
        (req, res) -> {
          assertThat(currentContextData()).isInstanceOf(ExecutionContext.UserContext.class);
          ExecutionContext.UserContext ctx = (ExecutionContext.UserContext) currentContextData();
          assertThat(ctx.userId()).isEqualTo(USER_ID);
          assertThat(MDC.get(MDCKeys.REQUEST_ID)).isEqualTo(REQUEST_ID);
          throw new IllegalStateException("boom");
        };

    assertThatThrownBy(() -> contextFilter.doFilter(request, response, filterChain))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("boom");

    assertThat(currentContextData()).isNull();
    assertThat(MDC.getCopyOfContextMap()).isNull();
  }

  private FilterChain chainAsserts(ThrowingAssertion assertion)
      throws ServletException, IOException {
    FilterChain filterChain = org.mockito.Mockito.mock(FilterChain.class);
    doAnswer(
            invocation -> {
              assertion.run();
              return null;
            })
        .when(filterChain)
        .doFilter(any(), any());
    return filterChain;
  }

  private JwtAuthenticationToken authenticationWithoutSub() {
    Instant now = Instant.now();
    Jwt jwt =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("email", SecurityFixtures.TEST_EMAIL)
            .claim("phone_number", SecurityFixtures.TEST_PHONE_NUMBER)
            .claim("active_organization", ACTIVE_ORGANIZATION)
            .claim("active_branch", null)
            .claim("groups", List.of())
            .claim("realm_access", Map.of("roles", List.of(UserRole.ORGANIZATION_ADMIN.name())))
            .claim("scope", "openid email profile")
            .build();
    return new JwtAuthenticationToken(jwt);
  }

  @SuppressWarnings("unchecked")
  private ExecutionContext.Context currentContextData() {
    ThreadLocal<ExecutionContext.Context> context =
        (ThreadLocal<ExecutionContext.Context>)
            ReflectionTestUtils.getField(ExecutionContext.class, "context");
    assert context != null;
    return context.get();
  }

  @FunctionalInterface
  private interface ThrowingAssertion {
    void run() throws Exception;
  }
}
