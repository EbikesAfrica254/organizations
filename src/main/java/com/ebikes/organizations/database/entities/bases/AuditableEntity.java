package com.ebikes.organizations.database.entities.bases;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@EntityListeners(AuditingEntityListener.class)
@Getter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class AuditableEntity extends BaseEntity {

  @CreatedBy
  @Column(name = "created_by", nullable = false, updatable = false, length = 36)
  private String createdBy;

  @LastModifiedDate
  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  @LastModifiedBy
  @Column(name = "updated_by", length = 36)
  private String updatedBy;
}
