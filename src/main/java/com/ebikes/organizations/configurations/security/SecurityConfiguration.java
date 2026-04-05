package com.ebikes.organizations.configurations.security;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.ebikes.organizations.configurations.filters.ContextFilter;
import com.ebikes.organizations.configurations.properties.SecurityProperties;
import com.ebikes.organizations.dtos.responses.api.ErrorResponse;
import com.ebikes.organizations.enums.ResponseCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfiguration {

  private final ObjectMapper objectMapper;
  private final SecurityProperties securityProperties;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder) {
    return http.csrf(AbstractHttpConfigurer::disable)
        .cors(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            req ->
                req.requestMatchers(securityProperties.getPublicEndpoints().toArray(String[]::new))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            server ->
                server
                    .jwt(
                        jwtConfigurer ->
                            jwtConfigurer
                                .decoder(decoder)
                                .jwtAuthenticationConverter(jwtAuthenticationConverter()))
                    .authenticationEntryPoint(
                        (request, response, authenticationException) -> {
                          ErrorResponse errorResponse =
                              ErrorResponse.from(
                                  authenticationException.getMessage(),
                                  UUID.randomUUID().toString(),
                                  request.getRequestURI(),
                                  ResponseCode.FORBIDDEN);
                          response.setStatus(HttpStatus.UNAUTHORIZED.value());
                          response.setContentType("application/json");
                          objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        }))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .addFilterAfter(new ContextFilter(), BearerTokenAuthenticationFilter.class)
        .build();
  }

  @Bean
  public JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new RealmRoleGrantedAuthoritiesConverter());
    return converter;
  }

  @Bean
  public JwtDecoder jwtDecoder(OAuth2ResourceServerProperties properties) {
    NimbusJwtDecoder jwtDecoder =
        NimbusJwtDecoder.withJwkSetUri(properties.getJwt().getJwkSetUri()).build();
    jwtDecoder.setJwtValidator(tokenValidator());
    return jwtDecoder;
  }

  @Bean
  public OAuth2TokenValidator<Jwt> tokenValidator() {
    return jwt -> {
      if (jwt.getExpiresAt() == null || jwt.getExpiresAt().isBefore(Instant.now())) {
        log.warn("Token validation: Expired JWT code");
        return createError(ResponseCode.SECURITY_CODE_EXPIRED, jwt.getIssuer().getPath());
      }
      String tokenScopesString = jwt.getClaimAsString("scope");
      List<String> tokenScopes =
          tokenScopesString == null ? null : List.of(tokenScopesString.split(" "));
      if (tokenScopes == null || tokenScopes.isEmpty()) {
        return createError(ResponseCode.INVALID_SECURITY_CODE, jwt.getIssuer().getPath());
      }

      boolean hasRequiredScope =
          securityProperties.getScopes().stream().anyMatch(tokenScopes::contains);
      if (!hasRequiredScope) {
        return createError(ResponseCode.INSUFFICIENT_SCOPE, jwt.getIssuer().getPath());
      }
      return OAuth2TokenValidatorResult.success();
    };
  }

  private OAuth2TokenValidatorResult createError(ResponseCode responseCode, String issuer) {
    BearerTokenError bearerTokenError =
        new BearerTokenError(
            responseCode.getCode(), HttpStatus.UNAUTHORIZED, responseCode.getUserMessage(), issuer);
    return OAuth2TokenValidatorResult.failure(bearerTokenError);
  }
}
