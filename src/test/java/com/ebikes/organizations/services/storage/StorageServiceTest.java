package com.ebikes.organizations.services.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.configurations.properties.AwsProperties;
import com.ebikes.organizations.dtos.internal.StoredFileMetadata;
import com.ebikes.organizations.dtos.internal.UploadUrlData;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

@DisplayName("StorageService")
@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

  private static final String BUCKET = "test-bucket";
  private static final String KEY = "documents/KRA_PIN_CERT/test-file.pdf";
  private static final String FILE_NAME = "test-file.pdf";
  private static final String CONTENT_TYPE = "application/pdf";
  private static final String PRESIGNED_URL = "https://test-bucket.s3.amazonaws.com/test-file";

  @Mock private S3Client s3Client;
  @Mock private S3Presigner s3Presigner;

  private StorageService service;

  @BeforeEach
  void setUp() {
    AwsProperties.S3 s3 = new AwsProperties.S3();
    s3.setBucketName(BUCKET);
    s3.setPresignedUrlExpiryMinutes(15);
    s3.setPreviewUrlExpiryMinutes(5);
    s3.setRegion("af-south-1");

    AwsProperties awsProperties = new AwsProperties();
    awsProperties.setS3(s3);

    service = new StorageService(awsProperties, s3Client, s3Presigner);
  }

  @Nested
  @DisplayName("generateDownloadUrl")
  class GenerateDownloadUrl {

    @Test
    @DisplayName("should return presigned URL with attachment content-disposition")
    @SuppressWarnings("unchecked")
    void shouldReturnPresignedUrlWithAttachmentDisposition() throws Exception {
      PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

      String result = service.generateDownloadUrl(KEY, FILE_NAME, Duration.ofMinutes(15));

      assertThat(result).isEqualTo(PRESIGNED_URL);
    }

    @Test
    @DisplayName("should sanitize filename before embedding in content-disposition")
    @SuppressWarnings("unchecked")
    void shouldSanitizeFilenameInDisposition() throws Exception {
      PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

      String result = service.generateDownloadUrl(KEY, "my file (1).pdf", Duration.ofMinutes(15));

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("generatePreviewUrl")
  class GeneratePreviewUrl {

    @Test
    @DisplayName("should return presigned URL with inline content-disposition")
    @SuppressWarnings("unchecked")
    void shouldReturnPresignedUrlWithInlineDisposition() throws Exception {
      PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(s3Presigner.presignGetObject(any(Consumer.class))).thenReturn(presigned);

      String result = service.generatePreviewUrl(KEY, Duration.ofMinutes(5));

      assertThat(result).isEqualTo(PRESIGNED_URL);
    }
  }

  @Nested
  @DisplayName("generateUploadUrl")
  class GenerateUploadUrl {

    @Test
    @DisplayName("should return UploadUrlData with URL, signed headers, and expiry")
    @SuppressWarnings("unchecked")
    void shouldReturnUploadUrlData() throws Exception {
      Instant expiry = Instant.now().plusSeconds(900);
      Map<String, List<String>> signedHeaders = Map.of("x-amz-meta-type", List.of("pdf"));

      PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(presigned.signedHeaders()).thenReturn(signedHeaders);
      when(presigned.expiration()).thenReturn(expiry);
      when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presigned);

      UploadUrlData result =
          service.generateUploadUrl(KEY, CONTENT_TYPE, 1024L, Map.of("type", "pdf"));

      assertThat(result.url()).isEqualTo(PRESIGNED_URL);
      assertThat(result.headers()).isEqualTo(signedHeaders);
      assertThat(result.expiryTime()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("should succeed when metadata map is null")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenMetadataNull() throws Exception {
      PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(presigned.signedHeaders()).thenReturn(Map.of());
      when(presigned.expiration()).thenReturn(Instant.now().plusSeconds(900));
      when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presigned);

      UploadUrlData result = service.generateUploadUrl(KEY, CONTENT_TYPE, 1024L, null);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("should succeed when metadata map is empty")
    @SuppressWarnings("unchecked")
    void shouldSucceedWhenMetadataEmpty() throws Exception {
      PresignedPutObjectRequest presigned = mock(PresignedPutObjectRequest.class);
      when(presigned.url()).thenReturn(URI.create(PRESIGNED_URL).toURL());
      when(presigned.signedHeaders()).thenReturn(Map.of());
      when(presigned.expiration()).thenReturn(Instant.now().plusSeconds(900));
      when(s3Presigner.presignPutObject(any(Consumer.class))).thenReturn(presigned);

      UploadUrlData result = service.generateUploadUrl(KEY, CONTENT_TYPE, 1024L, Map.of());

      assertThat(result).isNotNull();
    }
  }

  @Nested
  @DisplayName("getStoredFileMetadata")
  class GetStoredFileMetadata {

    @Test
    @DisplayName("should return metadata with exists=true when object found in S3")
    void shouldReturnMetadataWhenFound() {
      Instant lastModified = Instant.now();
      HeadObjectResponse response =
          HeadObjectResponse.builder()
              .contentLength(2048L)
              .contentType(CONTENT_TYPE)
              .lastModified(lastModified)
              .build();
      when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(response);

      StoredFileMetadata result = service.getStoredFileMetadata(KEY);

      assertThat(result.exists()).isTrue();
      assertThat(result.size()).isEqualTo(2048L);
      assertThat(result.contentType()).isEqualTo(CONTENT_TYPE);
      assertThat(result.lastModified()).isEqualTo(lastModified);
    }

    @Test
    @DisplayName("should return metadata with exists=false when NoSuchKeyException thrown")
    void shouldReturnNotFoundMetadataWhenKeyMissing() {
      when(s3Client.headObject(any(HeadObjectRequest.class)))
          .thenThrow(NoSuchKeyException.builder().build());

      StoredFileMetadata result = service.getStoredFileMetadata(KEY);

      assertThat(result.exists()).isFalse();
      assertThat(result.size()).isNull();
      assertThat(result.contentType()).isNull();
      assertThat(result.lastModified()).isNull();
    }
  }

  @Nested
  @DisplayName("sanitizeFilename")
  class SanitizeFilename {

    @Test
    @DisplayName("should replace special characters with underscores")
    void shouldReplaceSpecialCharacters() {
      assertThat(service.sanitizeFilename("my file (1).pdf")).isEqualTo("my_file__1_.pdf");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when filename is null")
    void shouldThrowWhenFilenameNull() {
      assertThatThrownBy(() -> service.sanitizeFilename(null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when filename is blank")
    void shouldThrowWhenFilenameBlank() {
      assertThatThrownBy(() -> service.sanitizeFilename("   "))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
