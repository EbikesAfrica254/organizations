package com.ebikes.organizations.dtos.internal;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record UploadUrlData(String url, Map<String, List<String>> headers, Instant expiryTime) {}
