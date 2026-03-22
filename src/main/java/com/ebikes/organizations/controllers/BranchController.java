package com.ebikes.organizations.controllers;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.BranchAddress;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.dtos.requests.branches.CreateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.DeactivateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.UpdateBranchRequest;
import com.ebikes.organizations.dtos.responses.api.SuccessResponse;
import com.ebikes.organizations.dtos.responses.branches.BranchAddressResponse;
import com.ebikes.organizations.dtos.responses.branches.BranchResponse;
import com.ebikes.organizations.dtos.responses.branches.BranchSummaryResponse;
import com.ebikes.organizations.mappers.BranchAddressMapper;
import com.ebikes.organizations.mappers.BranchMapper;
import com.ebikes.organizations.services.BranchAddressService;
import com.ebikes.organizations.services.BranchService;
import com.ebikes.organizations.services.OrganizationService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/organizations/{organizationId}/branches")
@RestController
public class BranchController {

  private final BranchAddressMapper branchAddressMapper;
  private final BranchAddressService branchAddressService;
  private final BranchMapper branchMapper;
  private final BranchService branchService;
  private final OrganizationService organizationService;

  @PreAuthorize(
      "hasAnyAuthority('CUSTOMER', 'ORGANIZATION_ADMIN', 'ORGANIZATION_MAKER', 'SYSTEM_ADMIN')")
  @PostMapping
  public ResponseEntity<SuccessResponse<BranchResponse>> create(
      @PathVariable UUID organizationId, @Valid @RequestBody CreateBranchRequest request) {
    Organization organization = organizationService.findById(organizationId);
    Branch branch = branchService.create(organization, request);
    BranchAddress branchAddress = branchAddressService.findBranchLocationByBranchId(branch.getId());
    BranchAddressResponse branchAddressResponse = branchAddressMapper.toResponse(branchAddress);
    BranchResponse response = branchMapper.toResponse(branch, branchAddressResponse);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Branch created successfully"));
  }

  @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
  @PatchMapping("/{branchId}/deactivate")
  public ResponseEntity<SuccessResponse<BranchResponse>> deactivate(
      @PathVariable UUID organizationId,
      @PathVariable UUID branchId,
      @Valid @RequestBody DeactivateBranchRequest request) {
    Branch branch = branchService.deactivate(organizationId, branchId, request.reason());
    BranchAddress branchAddress = branchAddressService.findBranchLocationByBranchId(branchId);
    BranchAddressResponse branchAddressResponse = branchAddressMapper.toResponse(branchAddress);
    BranchResponse response = branchMapper.toResponse(branch, branchAddressResponse);
    return ResponseEntity.ok(SuccessResponse.of(response, "Branch deactivated successfully"));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping("/{branchId}")
  public ResponseEntity<SuccessResponse<BranchResponse>> findById(
      @PathVariable UUID organizationId, @PathVariable UUID branchId) {
    Branch branch = branchService.findById(organizationId, branchId);
    BranchAddress branchAddress = branchAddressService.findBranchLocationByBranchId(branchId);
    BranchAddressResponse branchAddressResponse = branchAddressMapper.toResponse(branchAddress);
    BranchResponse response = branchMapper.toResponse(branch, branchAddressResponse);
    return ResponseEntity.ok(SuccessResponse.of(response));
  }

  @PreAuthorize("isAuthenticated()")
  @GetMapping
  public ResponseEntity<SuccessResponse<List<BranchSummaryResponse>>> findByOrganization(
      @PathVariable UUID organizationId,
      @RequestParam(defaultValue = "false") boolean includeInactive) {
    List<Branch> branches = branchService.findByOrganization(organizationId, includeInactive);
    List<BranchSummaryResponse> responses =
        branches.stream().map(branchMapper::toSummaryResponse).toList();
    return ResponseEntity.ok(SuccessResponse.of(responses));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')")
  @PatchMapping("/{branchId}/reinstate")
  public ResponseEntity<SuccessResponse<BranchResponse>> reinstate(
      @PathVariable UUID organizationId, @PathVariable UUID branchId) {
    Branch branch = branchService.reinstate(organizationId, branchId);
    BranchAddress branchAddress = branchAddressService.findBranchLocationByBranchId(branchId);
    BranchAddressResponse branchAddressResponse = branchAddressMapper.toResponse(branchAddress);
    BranchResponse response = branchMapper.toResponse(branch, branchAddressResponse);
    return ResponseEntity.ok(SuccessResponse.of(response, "Branch reinstated successfully"));
  }

  @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'SYSTEM_ADMIN')")
  @PatchMapping("/{branchId}/suspend")
  public ResponseEntity<SuccessResponse<BranchResponse>> suspend(
      @PathVariable UUID organizationId, @PathVariable UUID branchId) {
    Branch branch = branchService.suspend(organizationId, branchId);
    BranchAddress branchAddress = branchAddressService.findBranchLocationByBranchId(branchId);
    BranchAddressResponse branchAddressResponse = branchAddressMapper.toResponse(branchAddress);
    BranchResponse response = branchMapper.toResponse(branch, branchAddressResponse);
    return ResponseEntity.ok(SuccessResponse.of(response, "Branch suspended successfully"));
  }

  @PreAuthorize(
      "hasAnyAuthority('BRANCH_ADMIN', 'BRANCH_MAKER', 'CUSTOMER', 'ORGANIZATION_ADMIN',"
          + " 'ORGANIZATION_MAKER', 'SYSTEM_ADMIN')")
  @PutMapping("/{branchId}")
  public ResponseEntity<Void> update(
      @PathVariable UUID organizationId,
      @PathVariable UUID branchId,
      @Valid @RequestBody UpdateBranchRequest request) {
    branchService.update(organizationId, branchId, request);
    return ResponseEntity.noContent().build();
  }
}
