package com.ebikes.organizations.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.ebikes.organizations.database.entities.bases.BaseEntity;
import com.ebikes.organizations.enums.OutboxStatus;
import com.ebikes.organizations.enums.ResponseCode;
import com.ebikes.organizations.exceptions.ValidationException;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder
@Table(name = "outbox", schema = "organizations")
public class Outbox extends BaseEntity {

  @Column(name = "event_type", nullable = false, length = 100)
  @NotNull private String eventType;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  @JdbcTypeCode(SqlTypes.JSON)
  @NotNull private Object payload;

  @Builder.Default
  @Column(name = "retry_count", nullable = false)
  private Integer retryCount = 0;

  @Column(name = "routing_key", nullable = false, length = 200)
  @NotNull private String routingKey;

  @Builder.Default
  @Column(name = "status", nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  @NotNull private OutboxStatus status = OutboxStatus.PENDING;

  @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime updatedAt;

  public void markDeadLetter() {
    if (this.status != OutboxStatus.FAILED) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only FAILED outbox events can be marked as dead letter. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = OutboxStatus.DEAD_LETTER;
  }

  public void markFailed() {
    if (this.status != OutboxStatus.PENDING) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only PENDING outbox events can be marked as failed. Current status: " + this.status,
          "status",
          this.status);
    }
    this.retryCount++;
    this.status = OutboxStatus.FAILED;
  }

  public void markSent() {
    if (this.status != OutboxStatus.PENDING) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only PENDING outbox events can be marked as sent. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = OutboxStatus.SENT;
  }

  public void resetForRetry() {
    if (this.status != OutboxStatus.FAILED) {
      throw new ValidationException(
          ResponseCode.INVALID_STATE,
          "Only FAILED outbox events can be reset for retry. Current status: " + this.status,
          "status",
          this.status);
    }
    this.status = OutboxStatus.PENDING;
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
