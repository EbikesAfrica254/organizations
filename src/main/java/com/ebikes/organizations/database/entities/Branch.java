package com.ebikes.organizations.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "branches", schema = "organizations")
public class Branch extends AuditableEntity {

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

  @Column(name = "operating_hours", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  private List<DaySchedule> operatingHours = new ArrayList<>();

  @JoinColumn(name = "organization_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @NotNull @SuppressWarnings(
      "EI_EXPOSE_REP2") // JPA-managed association; defensive copy would break persistence context
  private Organization organization;

  @Column(name = "phone_number", nullable = false, length = 20)
  @NotBlank private String phoneNumber;

  @Column(nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private BranchStatus status;

  @Builder
  public Branch(
      @NotBlank String branchName,
      @NotBlank String displayName,
      @NotBlank String email,
      List<DaySchedule> operatingHours,
      @NotNull Organization organization,
      @NotBlank String phoneNumber) {

    this.branchName = branchName;
    this.displayName = displayName;
    this.email = email;
    this.operatingHours =
        operatingHours != null ? new ArrayList<>(operatingHours) : new ArrayList<>();
    this.organization = organization;
    this.phoneNumber = phoneNumber;
    this.status = BranchStatus.ACTIVE;
  }

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
