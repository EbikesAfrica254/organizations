package com.ebikes.organizations.dtos.responses.organizations;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ImageUploadInitiationResponse(
    Instant expiryTime, String key, Map<String, List<String>> signedHeaders, String url) {}
