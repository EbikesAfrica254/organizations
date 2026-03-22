package com.ebikes.organizations.dtos.responses.documents;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentUploadInitiationResponse(
    UUID documentId,
    Instant expiryTime,
    String key,
    Map<String, List<String>> signedHeaders,
    String url) {}
