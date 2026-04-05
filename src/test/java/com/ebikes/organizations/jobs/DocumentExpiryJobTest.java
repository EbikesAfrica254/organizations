package com.ebikes.organizations.jobs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.support.fixtures.DocumentFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;

@DisplayName("DocumentExpiryJob")
@ExtendWith(MockitoExtension.class)
class DocumentExpiryJobTest {

  @Mock private DocumentRepository documentRepository;
  @Mock private DocumentService documentService;
  @Mock private OrganizationService organizationService;

  private DocumentExpiryJob job;
  private Organization organization;

  @BeforeEach
  void setUp() {
    job = new DocumentExpiryJob(documentRepository, documentService, organizationService);

    organization = OrganizationFixtures.active();
    ReflectionTestUtils.setField(organization, "id", UUID.randomUUID());
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should do nothing when no expired documents exist")
    void shouldDoNothingWhenNothingToProcess() {
      when(documentRepository.findByStatusAndExpiryDateBefore(
              eq(DocumentStatus.ACTIVE), any(LocalDate.class)))
          .thenReturn(List.of());

      job.execute();

      verify(documentService, never()).expire(anyList());
      verify(organizationService, never()).updateComplianceStatus(any());
    }

    @Test
    @DisplayName("should expire documents and re-evaluate compliance for affected organizations")
    void shouldExpireDocumentsAndReevaluateCompliance() {
      Document expired = DocumentFixtures.active(organization);
      when(documentRepository.findByStatusAndExpiryDateBefore(
              eq(DocumentStatus.ACTIVE), any(LocalDate.class)))
          .thenReturn(List.of(expired));

      job.execute();

      verify(documentService).expire(List.of(expired));
      verify(organizationService).updateComplianceStatus(organization);
    }

    @Test
    @DisplayName("should deduplicate organizations when multiple documents from same org expire")
    void shouldDeduplicateOrganizationsForComplianceReevaluation() {
      Document first = DocumentFixtures.active(organization);
      Document second = DocumentFixtures.active(organization);
      when(documentRepository.findByStatusAndExpiryDateBefore(
              eq(DocumentStatus.ACTIVE), any(LocalDate.class)))
          .thenReturn(List.of(first, second));

      job.execute();

      verify(documentService).expire(List.of(first, second));
      verify(organizationService, times(1)).updateComplianceStatus(organization);
    }

    @Test
    @DisplayName("should skip compliance re-evaluation for documents with no organization")
    void shouldSkipComplianceForUnassociatedDocuments() {
      Document unassociated =
          Document.builder().documentType(DocumentType.KRA_PIN_CERTIFICATE).build();
      unassociated.markUploaded(1024L, "application/pdf");
      unassociated.activate();
      when(documentRepository.findByStatusAndExpiryDateBefore(
              eq(DocumentStatus.ACTIVE), any(LocalDate.class)))
          .thenReturn(List.of(unassociated));

      job.execute();

      verify(documentService).expire(List.of(unassociated));
      verify(organizationService, never()).updateComplianceStatus(any());
    }
  }
}
