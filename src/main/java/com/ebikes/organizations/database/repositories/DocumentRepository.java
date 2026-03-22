package com.ebikes.organizations.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ebikes.organizations.database.entities.Document;
import com.ebikes.organizations.enums.DocumentStatus;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

  List<Document> findAllByFileStorageUrlIn(List<String> storageKeys);

  List<Document> findByOrganizationId(UUID organizationId);

  List<Document> findByOrganizationIdAndStatus(UUID organizationId, DocumentStatus status);

  List<Document> findByOrganizationIdAndStatusIn(
      UUID organizationId, List<DocumentStatus> statuses);
}
