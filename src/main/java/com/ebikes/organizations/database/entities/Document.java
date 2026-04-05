package com.ebikes.organizations.database.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.ebikes.organizations.database.entities.bases.AuditableEntity;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.BusinessRuleException;
import com.ebikes.organizations.support.audit.Auditable;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "documents", schema = "organizations")
public class Document extends AuditableEntity implements Auditable {

  @Column(name = "document_type", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private DocumentType documentType;

  @Column(name = "expired_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime expiredAt;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(name = "file_name")
  private String fileName;

  @Column(name = "file_size_bytes")
  private Long fileSizeBytes;

  @Column(name = "file_storage_url", length = 1000)
  private String fileStorageUrl;

  @Column(name = "mime_type", length = 100)
  private String mimeType;

  @JoinColumn(name = "organization_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Organization organization;

  @JoinColumn(name = "replaces_document_id")
  @ManyToOne(fetch = FetchType.LAZY)
  private Document replacesDocument;

  @Builder.Default
  @Column(name = "status", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private DocumentStatus status = DocumentStatus.PENDING;

  @Column(name = "uploaded_at")
  private OffsetDateTime uploadedAt;

  @Column(nullable = false)
  @Version
  private Long version;

  public void activate() {
    if (this.status != DocumentStatus.UPLOADED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only UPLOADED documents can be activated. Current status: " + this.status);
    }
    this.status = DocumentStatus.ACTIVE;
  }

  public void assignStorageKey(@NotBlank String key) {
    this.fileStorageUrl = key;
  }

  public void associate(Organization organization) {
    if (organization == null) {
      throw new IllegalArgumentException("Organization is required");
    }

    if (this.organization != null && !this.organization.equals(organization)) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE, "Document is already associated with another organization");
    }

    this.organization = organization;
    organization.addDocument(this);
  }

  public void designateAsReplacementFor(@NotNull Document oldDocument) {
    this.replacesDocument = oldDocument;
  }

  public void markArchived() {
    if (this.status != DocumentStatus.UPLOADED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only UPLOADED documents can be archived. Current status: " + this.status);
    }
    this.status = DocumentStatus.ARCHIVED;
  }

  public void markExpired() {
    if (this.status != DocumentStatus.ACTIVE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE documents can be expired. Current status: " + this.status);
    }
    this.status = DocumentStatus.EXPIRED;
    this.expiredAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void markReplaced() {
    if (this.status != DocumentStatus.ACTIVE && this.status != DocumentStatus.EXPIRED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE or EXPIRED documents can be replaced. Current status: " + this.status);
    }
    this.status = DocumentStatus.REPLACED;
  }

  public void markUploaded(Long fileSizeBytes, String mimeType) {
    if (this.status != DocumentStatus.PENDING) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only PENDING documents can be marked as uploaded. Current status: " + this.status);
    }
    this.status = DocumentStatus.UPLOADED;
    this.uploadedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.fileSizeBytes = fileSizeBytes;
    this.mimeType = mimeType;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    return Map.of(
        "documentType", documentType.name(),
        "organizationId", organization != null ? organization.getId().toString() : "",
        "status", status.name());
  }
}
