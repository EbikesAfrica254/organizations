package com.ebikes.organizations.jobs;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.configurations.properties.DocumentProperties;
import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.support.jobs.ScheduledJobDecorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentArchivalJob {

  private final DocumentProperties documentProperties;
  private final DocumentRepository documentRepository;
  private final DocumentService documentService;

  @Scheduled(cron = "${document.archival-cron}")
  public void run() {
    ScheduledJobDecorator.decorate(this::execute);
  }

  @Transactional
  void execute() {
    OffsetDateTime staleThreshold =
        OffsetDateTime.now(ZoneOffset.UTC)
            .minusDays(documentProperties.getStaleUploadThresholdDays());

    List<Document> stale =
        documentRepository.findByStatusAndUploadedAtBefore(DocumentStatus.UPLOADED, staleThreshold);

    if (stale.isEmpty()) {
      log.debug("document-archival: nothing to process");
      return;
    }

    documentService.archive(stale);

    log.info("document-archival: archived={}", stale.size());
  }
}
