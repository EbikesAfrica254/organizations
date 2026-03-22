package com.ebikes.organizations.controllers;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ebikes.organizations.dtos.requests.documents.DocumentUploadConfirmationRequest;
import com.ebikes.organizations.dtos.requests.documents.DocumentUploadInitiationRequest;
import com.ebikes.organizations.dtos.responses.api.SuccessResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentResponse;
import com.ebikes.organizations.dtos.responses.documents.DocumentUploadInitiationResponse;
import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.services.DocumentService;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RequestMapping("/documents")
@RestController
public class DocumentController {

  private final DocumentService documentService;

  @PostMapping("/initiate-upload")
  public ResponseEntity<SuccessResponse<DocumentUploadInitiationResponse>> initiateUpload(
      @Valid @RequestBody DocumentUploadInitiationRequest request) {
    DocumentUploadInitiationResponse response =
        documentService.initiateUpload(
            request.documentType(), request.fileName(), request.contentType());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Upload initiated successfully."));
  }

  @GetMapping("/registration-types/{registrationType}/required-documents")
  public ResponseEntity<SuccessResponse<Set<DocumentType>>> getRequiredDocuments(
      @PathVariable BusinessRegistrationType registrationType) {
    Set<DocumentType> requiredDocuments = documentService.getRequiredDocuments(registrationType);
    return ResponseEntity.ok(SuccessResponse.of(requiredDocuments));
  }

  @PutMapping("/{id}/confirm-upload")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmUpload(
      @PathVariable UUID id, @Valid @RequestBody DocumentUploadConfirmationRequest request) {
    DocumentResponse response = documentService.confirmUpload(id, request);
    return ResponseEntity.ok(SuccessResponse.of(response, "Document upload confirmed."));
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<SuccessResponse<String>> download(@PathVariable UUID id) {
    String downloadUrl = documentService.download(id);
    return ResponseEntity.ok(SuccessResponse.of(downloadUrl, "Download URL generated"));
  }

  @PostMapping("/{id}/replace")
  public ResponseEntity<SuccessResponse<DocumentUploadInitiationResponse>> replaceDocument(
      @PathVariable UUID id, @Valid @RequestBody DocumentUploadInitiationRequest request) {
    DocumentUploadInitiationResponse response =
        documentService.replaceDocument(id, request.fileName(), request.contentType());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(SuccessResponse.of(response, "Document replacement initiated"));
  }

  @PutMapping("/{id}/confirm-replacement")
  public ResponseEntity<SuccessResponse<DocumentResponse>> confirmReplacement(
      @PathVariable UUID id, @Valid @RequestBody DocumentUploadConfirmationRequest request) {
    DocumentResponse response = documentService.confirmReplacement(id, request);
    return ResponseEntity.ok(
        SuccessResponse.of(response, "Document replacement confirmed and submitted for approval"));
  }
}
