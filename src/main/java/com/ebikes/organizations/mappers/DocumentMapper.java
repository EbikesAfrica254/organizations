package com.ebikes.organizations.mappers;

import java.time.Instant;
import java.util.UUID;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.dtos.internal.UploadUrlData;
import com.ebikes.organizations.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentUploadInitiationResponse;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

  @Mapping(target = "documentId", source = "documentId")
  @Mapping(target = "expiryTime", source = "uploadUrlData.expiryTime")
  @Mapping(target = "key", source = "key")
  @Mapping(target = "signedHeaders", source = "uploadUrlData.headers")
  @Mapping(target = "url", source = "uploadUrlData.url")
  DocumentUploadInitiationResponse toUploadInitiationResponse(
      UUID documentId, String key, UploadUrlData uploadUrlData);

  @Mapping(target = "organizationId", source = "organization.id")
  @Mapping(target = "replacesDocumentId", source = "replacesDocument.id")
  @Mapping(target = "updatedAt", source = "updatedAt")
  DocumentResponse toResponse(Document document);

  @Mapping(target = "organizationId", source = "organization.id")
  DocumentSummaryResponse toSummaryResponse(Document document);

  @Mapping(target = "organizationId", source = "doc.organization.id")
  @Mapping(target = "previewUrl", source = "previewUrl")
  @Mapping(target = "previewUrlExpiresAt", source = "previewUrlExpiresAt")
  DocumentPreviewResponse toPreviewResponse(
      Document doc, String previewUrl, Instant previewUrlExpiresAt);
}
