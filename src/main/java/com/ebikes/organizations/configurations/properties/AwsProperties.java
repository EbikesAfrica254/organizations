package com.ebikes.organizations.configurations.properties;

import java.util.Map;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import com.ebikes.organizations.enums.DocumentType;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "aws")
@Data
@Validated
public class AwsProperties {

  @NotNull private S3 s3;

  @Data
  public static class S3 {
    @NotNull private Map<DocumentType, String> allowedContentTypes;
    @NotBlank private String bucketName;
    private String endpoint;

    @NotNull @Min(1) private Integer maxFileSizeMb;

    @NotNull @Min(1) private Integer presignedUrlExpiryMinutes;

    @NotNull @Min(1) @Max(15) private Integer previewUrlExpiryMinutes;

    @NotBlank private String region;
  }
}
