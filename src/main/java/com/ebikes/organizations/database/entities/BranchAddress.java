package com.ebikes.organizations.database.entities;

import java.math.BigDecimal;

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

import com.ebikes.organizations.database.entities.bases.AuditableEntity;
import com.ebikes.organizations.dtos.requests.branches.BranchAddressRequest;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ValidationException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "branch_addresses", schema = "organizations")
@ToString(exclude = "branch")
public class BranchAddress extends AuditableEntity {

  @Column(name = "address_tag", nullable = false, length = 50)
  @Enumerated(EnumType.STRING)
  @NotNull private AddressTag addressTag;

  @JoinColumn(name = "branch_id", nullable = false, updatable = false)
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @NotNull private Branch branch;

  @Column(nullable = false, length = 100)
  @NotBlank private String city;

  @Column(nullable = false, length = 100)
  @NotBlank private String country;

  @Column(precision = 10, scale = 8)
  private BigDecimal latitude;

  @Column(precision = 11, scale = 8)
  private BigDecimal longitude;

  @Column(name = "postal_code", length = 20)
  private String postalCode;

  @Column(name = "street_address", nullable = false, length = 500)
  @NotBlank private String streetAddress;

  public void update(BranchAddressRequest request) {
    if (request.city() != null && !request.city().isBlank()) {
      this.city = request.city();
    }
    if (request.country() != null && !request.country().isBlank()) {
      this.country = request.country();
    }
    if (request.streetAddress() != null && !request.streetAddress().isBlank()) {
      this.streetAddress = request.streetAddress();
    }
    if (request.postalCode() != null) {
      this.postalCode = request.postalCode();
    }

    if ((request.latitude() == null) != (request.longitude() == null)) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Latitude and longitude must both be provided or both be absent",
          "coordinates",
          null);
    }
    if (request.latitude() != null) {
      this.latitude = request.latitude();
      this.longitude = request.longitude();
    }
  }
}
