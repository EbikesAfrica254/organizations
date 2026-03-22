package com.ebikes.organizations.database.entities;

import java.io.Serial;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox", schema = "organizations")
public class Inbox implements Serializable {

  @Serial private static final long serialVersionUID = 1L;

  @Column(name = "service_reference", nullable = false)
  @Id
  @NotBlank private String serviceReference;

  @Column(name = "event_type", nullable = false, length = 100)
  @NotBlank private String eventType;

  @Column(name = "processed_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime processedAt;

  @Column(name = "received_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime receivedAt;

  @Column(name = "source_context", nullable = false, length = 100)
  @NotBlank private String sourceContext;

  public Inbox(
      @NotBlank String eventType,
      @NotBlank String serviceReference,
      @NotBlank String sourceContext) {
    this.eventType = eventType;
    this.receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.serviceReference = serviceReference;
    this.sourceContext = sourceContext;
  }

  public void markProcessed() {
    if (this.processedAt != null) {
      throw new IllegalStateException(
          "Inbox record already marked as processed: " + this.serviceReference);
    }
    this.processedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
