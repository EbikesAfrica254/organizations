package com.ebikes.organizations.database.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.ebikes.organizations.database.entities.Outbox;
import com.ebikes.organizations.enums.OutboxStatus;

@Repository
public interface OutboxRepository
    extends JpaRepository<Outbox, UUID>, JpaSpecificationExecutor<Outbox> {

  List<Outbox> findByStatusOrderByIdAsc(OutboxStatus status);
}
