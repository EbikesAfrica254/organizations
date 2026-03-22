package com.ebikes.organizations.dtos.internal;

import java.time.Instant;

public record StoredFileMetadata(
    Long size, String contentType, Instant lastModified, boolean exists) {}
