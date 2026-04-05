package com.ebikes.organizations.configurations.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

class RealmRoleGrantedAuthoritiesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

  private static final String REALM_ACCESS_CLAIM = "realm_access";
  private static final String ROLES_CLAIM = "roles";

  private final JwtGrantedAuthoritiesConverter defaultConverter =
      new JwtGrantedAuthoritiesConverter();

  @Override
  public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
    Set<GrantedAuthority> authorities = new HashSet<>(defaultConverter.convert(jwt));

    Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS_CLAIM);
    if (realmAccess == null) {
      return authorities;
    }

    Object roles = realmAccess.get(ROLES_CLAIM);
    if (!(roles instanceof Collection<?> roleCollection)) {
      return authorities;
    }

    for (Object role : roleCollection) {
      if (role instanceof String roleName && !roleName.isBlank()) {
        authorities.add(new SimpleGrantedAuthority(roleName));
      }
    }

    return authorities;
  }
}
