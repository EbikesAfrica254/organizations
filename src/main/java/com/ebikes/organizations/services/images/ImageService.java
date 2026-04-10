package com.ebikes.organizations.services.images;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.dtos.internal.UploadUrlData;
import com.ebikes.organizations.dtos.requests.organizations.ImageUploadConfirmationRequest;
import com.ebikes.organizations.dtos.responses.organizations.ImageUploadInitiationResponse;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.services.storage.StorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
public class ImageService {

  private static final String LOGO_KEY_PREFIX = "logos/";
  private static final String LOGO_KEY_SUFFIX = "/logo";
  private static final Duration LOGO_PRESIGNED_TTL = Duration.ofHours(1);
  private static final Set<String> ALLOWED_MIME_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp");

  private final OrganizationService organizationService;
  private final StorageService storageService;

  public ImageUploadInitiationResponse generateImageUploadUrl(UUID organizationId) {
    organizationService.requireById(organizationId);
    String key = buildImageKey(organizationId);
    log.info("Generating logo upload URL: organizationId={}, key={}", organizationId, key);
    UploadUrlData uploadUrlData = storageService.generateUploadUrl(key, null, null, null);
    return new ImageUploadInitiationResponse(
        uploadUrlData.expiryTime(), key, uploadUrlData.headers(), uploadUrlData.url());
  }

  @Transactional
  public void confirmImageUpload(UUID organizationId, ImageUploadConfirmationRequest request) {
    validateMimeType(request.mimeType());

    String key = request.logoKey();

    var metadata = storageService.getStoredFileMetadata(key);
    if (!metadata.exists()) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Image file not found in storage — ensure upload completed before confirming",
          "logoKey",
          key);
    }

    String previousKey = organizationService.updateLogoKey(organizationId, key);

    if (previousKey != null) {
      log.info("Deleting previous logo: organizationId={}, key={}", organizationId, previousKey);
      storageService.deleteObject(previousKey);
    }

    log.info("Image confirmed: organizationId={}, key={}", organizationId, key);
  }

  public String generateImageRedirectUrl(UUID organizationId) {
    String logoKey = organizationService.requireById(organizationId).getLogoKey();

    if (logoKey == null) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Organization has no logo uploaded",
          "logoKey",
          organizationId);
    }

    log.debug("Generating logo redirect URL: organizationId={}", organizationId);
    return storageService.generatePreviewUrl(logoKey, LOGO_PRESIGNED_TTL);
  }

  private String buildImageKey(UUID organizationId) {
    return LOGO_KEY_PREFIX + organizationId + LOGO_KEY_SUFFIX + "-" + UUID.randomUUID();
  }

  private void validateMimeType(String mimeType) {
    if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Unsupported image MIME type '" + mimeType + "'. Allowed: " + ALLOWED_MIME_TYPES,
          "mimeType",
          mimeType);
    }
  }
}
