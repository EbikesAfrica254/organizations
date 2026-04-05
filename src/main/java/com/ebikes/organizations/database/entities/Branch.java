package com.ebikes.organizations.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ebikes.organizations.database.entities.bases.AuditableEntity;
import com.ebikes.organizations.database.models.DaySchedule;
import com.ebikes.organizations.enums.BranchStatus;
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
@Table(name = "branches", schema = "organizations")
public class Branch extends AuditableEntity implements Auditable {

  @Column(name = "branch_name", nullable = false)
  @NotBlank private String branchName;

  @Column(name = "deactivated_at")
  private OffsetDateTime deactivatedAt;

  @Column(name = "deactivation_reason")
  private String deactivationReason;

  @Column(name = "display_name", nullable = false)
  @NotBlank private String displayName;

  @Column(nullable = false)
  @NotBlank private String email;

  @Builder.Default
  @Column(name = "operating_hours", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<DaySchedule> operatingHours = new ArrayList<>();

  @JoinColumn(name = "organization_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @NotNull private Organization organization;

  @Column(name = "phone_number", nullable = false, length = 20)
  @NotBlank private String phoneNumber;

  @Builder.Default
  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private BranchStatus status = BranchStatus.ACTIVE;

  public void deactivate(String reason) {
    if (this.status != BranchStatus.ACTIVE && this.status != BranchStatus.SUSPENDED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE or SUSPENDED branches can be deactivated. Current status: " + this.status);
    }
    this.status = BranchStatus.DEACTIVATED;
    this.deactivatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.deactivationReason = reason;
  }

  public void reinstate() {
    if (this.status != BranchStatus.SUSPENDED) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only SUSPENDED branches can be reinstated. Current status: " + this.status);
    }
    this.status = BranchStatus.ACTIVE;
  }

  public void suspend() {
    if (this.status != BranchStatus.ACTIVE) {
      throw new BusinessRuleException(
          ResponseCode.INVALID_STATE,
          "Only ACTIVE branches can be suspended. Current status: " + this.status);
    }
    this.status = BranchStatus.SUSPENDED;
  }

  @Override
  public Map<String, String> toAuditMetadata() {
    return Map.of(
        "branchName",
        branchName,
        "displayName",
        displayName,
        "organizationId",
        String.valueOf(organization.getId()),
        "status",
        status.name());
  }

  public void update(
      String branchName,
      String displayName,
      String email,
      String phoneNumber,
      List<DaySchedule> operatingHours) {
    if (branchName != null && !branchName.isBlank()) {
      this.branchName = branchName;
    }
    if (displayName != null && !displayName.isBlank()) {
      this.displayName = displayName;
    }
    if (email != null && !email.isBlank()) {
      this.email = email;
    }
    if (phoneNumber != null && !phoneNumber.isBlank()) {
      this.phoneNumber = phoneNumber;
    }
    if (operatingHours != null) {
      this.operatingHours = new ArrayList<>(operatingHours);
    }
  }
}
