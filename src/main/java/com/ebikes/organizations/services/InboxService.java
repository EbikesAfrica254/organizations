package com.ebikes.organizations.services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import com.ebikes.organizations.database.entities.Inbox;
import com.ebikes.organizations.database.repositories.InboxRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Service
@Slf4j
@Validated
public class InboxService {

  private final InboxRepository inboxRepository;

  @Transactional
  public void markProcessed(String serviceReference) {
    Inbox inbox =
        inboxRepository
            .findById(serviceReference)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Cannot mark processed — inbox record not found: " + serviceReference));
    inbox.markProcessed();
    inboxRepository.save(inbox);
    log.debug("Inbox record marked as processed: serviceReference={}", serviceReference);
  }

  @Transactional
  public boolean receive(String eventType, String serviceReference, String sourceContext) {
    try {
      Inbox inbox = new Inbox(eventType, serviceReference, sourceContext);
      inboxRepository.save(inbox);
      log.debug(
          "Inbox record created: eventType={}, serviceReference={}", eventType, serviceReference);
      return true;
    } catch (DataIntegrityViolationException e) {
      log.debug("Duplicate event detected, skipping: serviceReference={}", serviceReference);
      return false;
    }
  }
}
