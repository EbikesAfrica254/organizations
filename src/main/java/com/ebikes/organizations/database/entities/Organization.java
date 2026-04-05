package com.ebikes.organizations.database.entities;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ebikes.organizations.database.entities.bases.AuditableEntity;
import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.ComplianceStatus;
import com.ebikes.organizations.enums.OrganizationStatus;
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
@Table(name = "organizations", schema = "organizations")
public class Organization extends AuditableEntity implements Auditable {

  @Column(name = "activated_at")
  private OffsetDateTime activatedAt;

  @Builder.Default
  @Column(name = "addresses", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<Address> addresses = new ArrayList<>();

  @Column(name = "approved_at")
  private OffsetDateTime approvedAt;

  @Builder.Default
  @Column(name = "compliance_status", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @NotNull private ComplianceStatus complianceStatus = ComplianceStatus.NON_COMPLIANT;

  @Column(name = "deactivated_at")
  private OffsetDateTime deactivatedAt;

  @Column(name = "display_name", nullable = false)
  @NotBlank private String displayName;

  @OneToMany(mappedBy = "organization", cascade = CascadeType.ALL, orphanRemoval = true)
  private final Set<Document> documents = new HashSet<>();

  @Column(nullable = false)
  @NotBlank private String email;

  @Column(name = "incorporation_date")
  private LocalDate incorporationDate;

  @Column(name = "kra_pin", length = 11)
  private String kraPin;

  @Column(name = "legal_name", nullable = false)
  @NotBlank private String legalName;

  @Column(name = "owner_id", nullable = false, length = 36)
  @NotBlank private String ownerId;

  @Column(name = "phone_number", nullable = false, length = 20)
  @NotBlank private String phoneNumber;

  @Column(name = "registration_number", length = 100)
  private String registrationNumber;

  @Column(name = "registration_type", nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @NotNull private BusinessRegistrationType registrationType;

  @Column(name = "rejection_reason")
  private String rejectionReason;

  @Builder.Default
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @NotNull private OrganizationStatus status = OrganizationStatus.PENDING_APPROVAL;

  @Column(name = "version", nullable = false)
  @Version
  private int version;

  public void approve() {
    if (this.status != OrganizationStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only organizations with PENDING_APPROVAL status can be approved. Current status: "
              + this.status);
    }
    this.status = OrganizationStatus.APPROVED;
    this.approvedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  public void activate() {
    if (this.status != OrganizationStatus.APPROVED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only organizations with APPROVED status can be activated. Current status: "
              + this.status);
    }
    this.status = OrganizationStatus.ACTIVE;
    this.activatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }

  void addDocument(Document document) {
    this.documents.add(document);
  }

  public void deactivate(String reason) {
    if (this.status != OrganizationStatus.ACTIVE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE organizations can be deactivated. Current status: " + this.status);
    }
    this.status = OrganizationStatus.DEACTIVATED;
    this.deactivatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.rejectionReason = reason;
  }

  public void updateComplianceStatus(@NotNull ComplianceStatus complianceStatus) {
    if (complianceStatus == ComplianceStatus.SUSPENDED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Compliance suspension requires explicit admin action and cannot be set"
              + " programmatically");
    }
    this.complianceStatus = complianceStatus;
  }

  public void reject(String reason) {
    if (this.status != OrganizationStatus.PENDING_APPROVAL) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only organizations with PENDING_APPROVAL status can be rejected. Current status: "
              + this.status);
    }
    this.status = OrganizationStatus.REJECTED;
    this.rejectionReason = reason;
  }

  public void resubmit() {
    if (this.status != OrganizationStatus.REJECTED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only REJECTED organizations can be resubmitted. Current status: " + this.status);
    }
    this.status = OrganizationStatus.PENDING_APPROVAL;
    this.rejectionReason = null;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    return Map.of(
        "complianceStatus", complianceStatus.name(),
        "displayName", displayName,
        "ownerId", ownerId,
        "registrationType", registrationType.name(),
        "status", status.name());
  }
}
