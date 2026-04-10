package com.ebikes.organizations.services.images;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.internal.StoredFileMetadata;
import com.ebikes.organizations.dtos.internal.UploadUrlData;
import com.ebikes.organizations.dtos.requests.organizations.ImageUploadConfirmationRequest;
import com.ebikes.organizations.dtos.responses.organizations.ImageUploadInitiationResponse;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;

@DisplayName("ImageService")
@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

  private static final UUID ORG_ID = UUID.randomUUID();
  private static final String STORED_LOGO_KEY = "logos/" + ORG_ID + "/logo-abc123";

  @Mock private OrganizationService organizationService;
  @Mock private StorageService storageService;

  private ImageService service;
  private Organization organization;

  @BeforeEach
  void setUp() {
    service = new ImageService(organizationService, storageService);
    organization = OrganizationFixtures.active();
    ReflectionTestUtils.setField(organization, "id", ORG_ID);
  }

  @Nested
  @DisplayName("generateImageUploadUrl")
  class GenerateImageUploadUrl {

    @Test
    @DisplayName("should verify org exists, generate versioned key, and return initiation response")
    void shouldVerifyOrgAndReturnInitiationResponse() {
      UploadUrlData uploadUrlData = new UploadUrlData("https://s3.example.com/upload", null, null);
      when(organizationService.requireById(ORG_ID)).thenReturn(organization);
      when(storageService.generateUploadUrl(any(String.class), eq(null), eq(null), eq(null)))
          .thenReturn(uploadUrlData);

      ImageUploadInitiationResponse response = service.generateImageUploadUrl(ORG_ID);

      verify(organizationService).requireById(ORG_ID);
      verify(storageService).generateUploadUrl(any(String.class), eq(null), eq(null), eq(null));
      assertThat(response.key()).startsWith("logos/" + ORG_ID + "/logo-");
      assertThat(response.url()).isEqualTo("https://s3.example.com/upload");
    }
  }

  @Nested
  @DisplayName("confirmImageUpload")
  class ConfirmImageUpload {

    @Test
    @DisplayName("should confirm upload and skip deletion when no previous key")
    void shouldConfirmAndSkipDeletionWhenNoPreviousKey() {
      String logoKey = "logos/" + ORG_ID + "/logo-new";
      ImageUploadConfirmationRequest request =
          new ImageUploadConfirmationRequest(102400L, logoKey, "image/jpeg");
      StoredFileMetadata metadata = new StoredFileMetadata(102400L, "image/jpeg", null, true);

      when(storageService.getStoredFileMetadata(logoKey)).thenReturn(metadata);
      when(organizationService.updateLogoKey(ORG_ID, logoKey)).thenReturn(null);

      service.confirmImageUpload(ORG_ID, request);

      verify(storageService).getStoredFileMetadata(logoKey);
      verify(organizationService).updateLogoKey(ORG_ID, logoKey);
      verify(storageService, never()).deleteObject(any());
    }

    @Test
    @DisplayName("should confirm upload and delete previous key when one exists")
    void shouldConfirmAndDeletePreviousKeyWhenPresent() {
      String logoKey = "logos/" + ORG_ID + "/logo-new";
      String previousKey = "logos/" + ORG_ID + "/logo-old";
      ImageUploadConfirmationRequest request =
          new ImageUploadConfirmationRequest(102400L, logoKey, "image/png");
      StoredFileMetadata metadata = new StoredFileMetadata(102400L, "image/png", null, true);

      when(storageService.getStoredFileMetadata(logoKey)).thenReturn(metadata);
      when(organizationService.updateLogoKey(ORG_ID, logoKey)).thenReturn(previousKey);

      service.confirmImageUpload(ORG_ID, request);

      verify(storageService).deleteObject(previousKey);
    }

    @Test
    @DisplayName("should throw ValidationException when mime type is not allowed")
    void shouldThrowWhenMimeTypeNotAllowed() {
      ImageUploadConfirmationRequest request =
          new ImageUploadConfirmationRequest(102400L, "logos/" + ORG_ID + "/logo-new", "image/gif");

      assertThatThrownBy(() -> service.confirmImageUpload(ORG_ID, request))
          .isInstanceOf(ValidationException.class);

      verify(storageService, never()).getStoredFileMetadata(any());
      verify(organizationService, never()).updateLogoKey(any(), any());
    }

    @Test
    @DisplayName("should throw ValidationException when file not found in storage")
    void shouldThrowWhenFileNotFoundInStorage() {
      String logoKey = "logos/" + ORG_ID + "/logo-new";
      ImageUploadConfirmationRequest request =
          new ImageUploadConfirmationRequest(102400L, logoKey, "image/webp");
      StoredFileMetadata metadata = new StoredFileMetadata(null, null, null, false);

      when(storageService.getStoredFileMetadata(logoKey)).thenReturn(metadata);

      assertThatThrownBy(() -> service.confirmImageUpload(ORG_ID, request))
          .isInstanceOf(ValidationException.class);

      verify(organizationService, never()).updateLogoKey(any(), any());
    }
  }

  @Nested
  @DisplayName("generateImageRedirectUrl")
  class GenerateImageRedirectUrl {

    @Test
    @DisplayName("should return presigned URL when org has a logo key")
    void shouldReturnPresignedUrlWhenLogoKeyPresent() {
      String presignedUrl = "https://s3.example.com/logos/presigned";
      ReflectionTestUtils.setField(organization, "logoKey", STORED_LOGO_KEY);

      when(organizationService.requireById(ORG_ID)).thenReturn(organization);
      when(storageService.generatePreviewUrl(eq(STORED_LOGO_KEY), any())).thenReturn(presignedUrl);

      String result = service.generateImageRedirectUrl(ORG_ID);

      assertThat(result).isEqualTo(presignedUrl);
      verify(storageService).generatePreviewUrl(eq(STORED_LOGO_KEY), any());
    }

    @Test
    @DisplayName("should throw ValidationException when org has no logo key")
    void shouldThrowWhenNoLogoKey() {
      when(organizationService.requireById(ORG_ID)).thenReturn(organization);

      assertThatThrownBy(() -> service.generateImageRedirectUrl(ORG_ID))
          .isInstanceOf(ValidationException.class);

      verify(storageService, never()).generatePreviewUrl(any(), any());
    }
  }
}
