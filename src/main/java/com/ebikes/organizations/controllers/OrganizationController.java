package com.ebikes.organizations.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.requests.filters.OrganizationFilter;
import com.ebikes.organizations.dtos.requests.organizations.CreateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.DeactivateOrganizationRequest;
import com.ebikes.organizations.dtos.requests.organizations.UpdateOrganizationRequest;
import com.ebikes.organizations.dtos.responses.api.PaginatedResponse;
import com.ebikes.organizations.dtos.responses.api.SuccessResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentPreviewResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentSummaryResponse;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationResponse;
import com.ebikes.organizations.dtos.responses.organizations.OrganizationSummaryResponse;
import com.ebikes.organizations.mappers.OrganizationMapper;
import com.ebikes.organizations.services.DocumentService;
import com.ebikes.organizations.services.OrganizationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/organizations")
@RestController
public class OrganizationController {

  private final DocumentService documentService;
  private final OrganizationMapper organizationMapper;
  private final OrganizationService organizationService;

  @PreAuthorize("hasAnyAuthority('CUSTOMER', 'SYSTEM_ADMIN')")
  @PostMapping
  public ResponseEntity<SuccessResponse<OrganizationResponse>> create(
      @Valid @RequestBody CreateOrganizationRequest request) {
    Organization organization = organizationService.create(request);
    OrganizationResponse response = organizationMapper.toResponse(organization);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Organization created successfully"));
  }

  @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
  @PatchMapping("/{id}/deactivate")
  public ResponseEntity<SuccessResponse<OrganizationResponse>> deactivate(
      @PathVariable UUID id, @Valid @RequestBody DeactivateOrganizationRequest request) {
    Organization organization = organizationService.deactivate(id, request.reason());
    OrganizationResponse response = organizationMapper.toResponse(organization);
    return ResponseEntity.ok(SuccessResponse.of(response, "Organization deactivated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}")
  public ResponseEntity<SuccessResponse<OrganizationResponse>> findById(@PathVariable UUID id) {
    Organization organization = organizationService.findById(id);
    OrganizationResponse response = organizationMapper.toResponse(organization);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/documents/previews")
  public ResponseEntity<SuccessResponse<List<DocumentPreviewResponse>>> findDocumentPreviews(
      @PathVariable UUID id, @RequestParam(defaultValue = "false") boolean includeInactive) {
    List<DocumentPreviewResponse> previews =
        documentService.findByOrganizationWithPreviews(id, includeInactive);
    return ResponseEntity.ok(SuccessResponse.of(previews));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{id}/documents")
  public ResponseEntity<SuccessResponse<List<DocumentSummaryResponse>>> findDocuments(
      @PathVariable UUID id, @RequestParam(defaultValue = "false") boolean includeInactive) {
    List<DocumentSummaryResponse> documents =
        documentService.findByOrganization(id, includeInactive);
    return ResponseEntity.ok(SuccessResponse.of(documents));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<PaginatedResponse<OrganizationSummaryResponse>> search(
      @ModelAttribute OrganizationFilter filter) {
    Page<Organization> page = organizationService.search(filter);
    Page<OrganizationSummaryResponse> responsePage =
        page.map(organizationMapper::toSummaryResponse);
    return ResponseEntity.ok(PaginatedResponse.from("Organizations retrieved", responsePage));
  }

  @PreAuthorize(
      "hasAnyAuthority('CUSTOMER', 'ORGANIZATION_ADMIN', 'ORGANIZATION_MAKER', 'SYSTEM_ADMIN')")
  @PutMapping("/{id}")
  public ResponseEntity<Void> update(
      @PathVariable UUID id, @Valid @RequestBody UpdateOrganizationRequest request) {
    organizationService.update(id, request);
    return ResponseEntity.noContent().build();
  }
}
