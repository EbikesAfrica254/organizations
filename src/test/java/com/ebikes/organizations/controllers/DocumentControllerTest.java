package com.ebikes.organizations.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.organizations.enums.BusinessRegistrationType;
import com.ebikes.organizations.enums.DocumentType;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.support.fixtures.DocumentRequestFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("DocumentController")
@WebMvcTest(DocumentController.class)
class DocumentControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private DocumentService documentService;

  private static final UUID DOCUMENT_ID = UUID.randomUUID();

  @Nested
  @DisplayName("POST /documents/initiate-upload")
  class InitiateUpload {

    @Test
    @DisplayName("should return 201 when request is valid")
    void shouldReturn201WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/documents/initiate-upload")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(DocumentRequestFixtures.initiationRequest())))
          .andExpect(status().isCreated());

      verify(documentService).initiateUpload(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              post("/documents/initiate-upload")
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /documents/registration-types/{registrationType}/required-documents")
  class GetRequiredDocuments {

    @Test
    @DisplayName("should return 200 for a known registration type")
    void shouldReturn200ForKnownRegistrationType() throws Exception {
      when(documentService.getRequiredDocuments(any()))
          .thenReturn(Set.of(DocumentType.BUSINESS_NAME_CERTIFICATE));

      mockMvc
          .perform(
              get(
                      "/documents/registration-types/{registrationType}/required-documents",
                      BusinessRegistrationType.PRIVATE_LIMITED_COMPANY)
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).getRequiredDocuments(any());
    }
  }

  @Nested
  @DisplayName("PUT /documents/{id}/confirm-upload")
  class ConfirmUpload {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-upload", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          DocumentRequestFixtures.confirmationRequest())))
          .andExpect(status().isOk());

      verify(documentService).confirmUpload(any(), any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-upload", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("GET /documents/{id}/download")
  class Download {

    @Test
    @DisplayName("should return 200 when document exists")
    void shouldReturn200WhenDocumentExists() throws Exception {
      when(documentService.download(any())).thenReturn("https://storage.example.com/file");

      mockMvc
          .perform(get("/documents/{id}/download", DOCUMENT_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).download(any());
    }
  }

  @Nested
  @DisplayName("POST /documents/{id}/replace")
  class ReplaceDocument {

    @Test
    @DisplayName("should return 201 when request is valid")
    void shouldReturn201WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              post("/documents/{id}/replace", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(DocumentRequestFixtures.initiationRequest())))
          .andExpect(status().isCreated());

      verify(documentService).replaceDocument(any(), any(), any(), any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              post("/documents/{id}/replace", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("PUT /documents/{id}/confirm-replacement")
  class ConfirmReplacement {

    @Test
    @DisplayName("should return 200 when request is valid")
    void shouldReturn200WhenRequestIsValid() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-replacement", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(
                          DocumentRequestFixtures.confirmationRequest())))
          .andExpect(status().isOk());

      verify(documentService).confirmReplacement(any(), any());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              put("/documents/{id}/confirm-replacement", DOCUMENT_ID)
                  .with(authenticatedJwt())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }
  }
}
