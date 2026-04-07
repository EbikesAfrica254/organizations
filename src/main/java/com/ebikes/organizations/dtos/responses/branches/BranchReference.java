package com.ebikes.organizations.dtos.responses.branches;

import java.util.UUID;

public record BranchReference(UUID id, String displayName, String logoUrl) {}
