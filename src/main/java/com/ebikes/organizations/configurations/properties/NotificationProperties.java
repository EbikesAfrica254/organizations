package com.ebikes.organizations.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import lombok.Data;

@ConfigurationProperties(prefix = "notifications")
@Component
@Data
@Validated
public class NotificationProperties {

  private boolean enabled = true;
}
