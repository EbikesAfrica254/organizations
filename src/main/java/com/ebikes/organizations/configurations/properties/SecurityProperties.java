package com.ebikes.organizations.configurations.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "security")
@Validated
@Data
public class SecurityProperties {
  private List<String> publicEndpoints;
  private List<String> scopes;
}
