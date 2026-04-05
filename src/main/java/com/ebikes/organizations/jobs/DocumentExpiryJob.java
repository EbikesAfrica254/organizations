package com.ebikes.organizations.jobs;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.repositories.DocumentRepository;
import com.ebikes.organizations.enums.DocumentStatus;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.support.jobs.ScheduledJobDecorator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentExpiryJob {

  private final DocumentRepository documentRepository;
  private final DocumentService documentService;
  private final OrganizationService organizationService;

  @Scheduled(cron = "${document.expiry-cron}")
  public void run() {
    ScheduledJobDecorator.decorate(this::execute);
  }

  @Transactional
  void execute() {
    LocalDate today = LocalDate.now(ZoneOffset.UTC);

    List<Document> expired =
        documentRepository.findByStatusAndExpiryDateBefore(DocumentStatus.ACTIVE, today);

    if (expired.isEmpty()) {
      log.debug("document-expiry: nothing to process");
      return;
    }

    documentService.expire(expired);

    Set<Organization> affectedOrganizations =
        expired.stream()
            .map(Document::getOrganization)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    affectedOrganizations.forEach(organizationService::updateComplianceStatus);

    log.info(
        "document-expiry: expired={}, orgs-affected={}",
        expired.size(),
        affectedOrganizations.size());
  }
}
