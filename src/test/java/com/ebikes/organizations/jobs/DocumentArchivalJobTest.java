package com.ebikes.organizations.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ebikes.organizations.configurations.properties.DocumentProperties;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.support.fixtures.DocumentFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;

@DisplayName("DocumentArchivalJob")
@ExtendWith(MockitoExtension.class)
class DocumentArchivalJobTest {

  @Mock private DocumentRepository documentRepository;
  @Mock private DocumentService documentService;

  private DocumentArchivalJob job;
  private Organization organization;

  @BeforeEach
  void setUp() {
    DocumentProperties properties = new DocumentProperties();
    properties.setStaleUploadThresholdDays(7);

    job = new DocumentArchivalJob(properties, documentRepository, documentService);
    organization = OrganizationFixtures.active();
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should do nothing when no stale uploads exist")
    void shouldDoNothingWhenNothingToProcess() {
      when(documentRepository.findByStatusAndUploadedAtBefore(
              eq(DocumentStatus.UPLOADED), any(OffsetDateTime.class)))
          .thenReturn(List.of());

      job.execute();

      verify(documentService, never()).archive(anyList());
    }

    @Test
    @DisplayName("should archive stale uploaded documents")
    void shouldArchiveStaleUploads() {
      Document stale = DocumentFixtures.uploaded(organization);
      when(documentRepository.findByStatusAndUploadedAtBefore(
              eq(DocumentStatus.UPLOADED), any(OffsetDateTime.class)))
          .thenReturn(List.of(stale));

      job.execute();

      verify(documentService).archive(List.of(stale));
    }

    @Test
    @DisplayName("should archive multiple stale documents in a single batch")
    void shouldArchiveMultipleStaleUploadsInBatch() {
      Document first = DocumentFixtures.uploaded(organization);
      Document second = DocumentFixtures.uploaded(organization);
      when(documentRepository.findByStatusAndUploadedAtBefore(
              eq(DocumentStatus.UPLOADED), any(OffsetDateTime.class)))
          .thenReturn(List.of(first, second));

      job.execute();

      verify(documentService).archive(List.of(first, second));
    }
  }
}
