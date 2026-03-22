package com.ebikes.organizations.configurations;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.ebikes.organizations.configurations.properties.AwsProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AwsConfiguration {

  private final AwsProperties awsProperties;

  @Bean(destroyMethod = "close")
  public S3Client s3Client() {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(awsProperties.getS3().getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build());

    if (awsProperties.getS3().getEndpoint() != null
        && !awsProperties.getS3().getEndpoint().isEmpty()) {
      URI endpointUri = URI.create(awsProperties.getS3().getEndpoint());
      builder
          .endpointOverride(endpointUri)
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());

      log.info("S3Client configured with endpoint override: {} (path-style enabled)", endpointUri);
    } else {
      log.info(
          "S3Client configured for AWS region: {} (virtual-hosted-style)",
          awsProperties.getS3().getRegion());
    }

    return builder.build();
  }

  @Bean(destroyMethod = "close")
  public S3Presigner s3Presigner() {
    S3Presigner.Builder builder =
        S3Presigner.builder()
            .region(Region.of(awsProperties.getS3().getRegion()))
            .credentialsProvider(DefaultCredentialsProvider.builder().build());

    if (awsProperties.getS3().getEndpoint() != null
        && !awsProperties.getS3().getEndpoint().isEmpty()) {
      URI endpointUri = URI.create(awsProperties.getS3().getEndpoint());
      builder
          .endpointOverride(endpointUri)
          .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());

      log.info("S3Presigner configured with endpoint override: {}", endpointUri);
    } else {
      log.info("S3Presigner configured for AWS region: {}", awsProperties.getS3().getRegion());
    }

    return builder.build();
  }
}
