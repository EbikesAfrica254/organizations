package com.ebikes.organizations.configurations.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "document")
@Getter
@Setter
public class DocumentProperties {

  private String expiryCron;
  private int staleUploadThresholdDays;
}
