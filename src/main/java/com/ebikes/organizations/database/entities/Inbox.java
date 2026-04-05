package com.ebikes.organizations.database.entities;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;

import org.springframework.data.domain.Persistable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "inbox", schema = "organizations")
public class Inbox implements Persistable<String> {

  @Column(name = "event_type", nullable = false, length = 100)
  private String eventType;

  @Column(name = "processed_at", columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime processedAt;

  @Column(name = "received_at", nullable = false, columnDefinition = "TIMESTAMPTZ")
  private OffsetDateTime receivedAt;

  @Column(name = "service_reference", nullable = false)
  @Id
  private String serviceReference;

  @Column(name = "source_context", nullable = false, length = 100)
  private String sourceContext;

  @Transient private boolean isNew = true;

  public Inbox(
      @NotBlank String eventType,
      @NotBlank String serviceReference,
      @NotBlank String sourceContext) {
    this.eventType = eventType;
    this.receivedAt = OffsetDateTime.now(ZoneOffset.UTC);
    this.serviceReference = serviceReference;
    this.sourceContext = sourceContext;
  }

  @Override
  public String getId() {
    return this.serviceReference;
  }

  @Override
  public boolean isNew() {
    return isNew;
  }

  @PostLoad
  @PostPersist
  void markNotNew() {
    this.isNew = false;
  }

  public void markProcessed() {
    if (this.processedAt != null) {
      throw new IllegalStateException(
          "Inbox record already marked as processed: " + this.serviceReference);
    }
    this.processedAt = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
