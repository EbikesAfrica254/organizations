package com.ebikes.organizations.configurations.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;

@DisplayName("RealmRoleGrantedAuthoritiesConverter")
class RealmRoleGrantedAuthoritiesConverterTest {

  private RealmRoleGrantedAuthoritiesConverter converter;

  @BeforeEach
  void setUp() {
    converter = new RealmRoleGrantedAuthoritiesConverter();
  }

  @Test
  @DisplayName("should return only default authorities when realm_access claim is null")
  void shouldReturnDefaultAuthoritiesWhenRealmAccessIsNull() {
    Jwt jwt =
        SecurityFixtures.jwt(
            SecurityFixtures.TEST_USER_ID,
            SecurityFixtures.TEST_ORGANIZATION_ID,
            null,
            List.of(),
            null);

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .isNotEmpty()
        .extracting(GrantedAuthority::getAuthority)
        .doesNotContain(UserRole.ORGANIZATION_ADMIN.name());
    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .doesNotContain(UserRole.ORGANIZATION_ADMIN.name());
  }

  @Test
  @DisplayName("should return only default authorities when roles is not a Collection")
  void shouldReturnDefaultAuthoritiesWhenRolesIsNotCollection() {
    Jwt jwt =
        SecurityFixtures.jwt(
            SecurityFixtures.TEST_USER_ID,
            SecurityFixtures.TEST_ORGANIZATION_ID,
            null,
            List.of(),
            Map.of("roles", "not-a-collection"));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .isNotEmpty()
        .extracting(GrantedAuthority::getAuthority)
        .doesNotContain("not-a-collection");
    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .doesNotContain("not-a-collection");
  }

  @Test
  @DisplayName("should add SimpleGrantedAuthority for each valid role")
  void shouldAddGrantedAuthorityForEachValidRole() {
    Jwt jwt = SecurityFixtures.jwt(UserRole.ORGANIZATION_ADMIN, UserRole.SYSTEM_ADMIN);

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains(UserRole.ORGANIZATION_ADMIN.name(), UserRole.SYSTEM_ADMIN.name());
  }

  @Test
  @DisplayName("should skip blank role strings")
  void shouldSkipBlankRoles() {
    Jwt jwt = buildJwtWithRoles(List.of("ORGANIZATION_ADMIN", "   ", ""));

    Collection<GrantedAuthority> authorities = converter.convert(jwt);

    assertThat(authorities)
        .extracting(GrantedAuthority::getAuthority)
        .contains("ORGANIZATION_ADMIN")
        .doesNotContain("   ", "");
  }

  private Jwt buildJwtWithRoles(List<String> roles) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .subject(SecurityFixtures.TEST_USER_ID)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(3600))
        .claim("scope", "openid")
        .claim("realm_access", Map.of("roles", roles))
        .build();
  }
}
