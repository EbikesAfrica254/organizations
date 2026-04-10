package com.ebikes.organizations.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.organizations.dtos.requests.organizations.ImageUploadConfirmationRequest;
import com.ebikes.organizations.dtos.responses.organizations.ImageUploadInitiationResponse;
import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.mappers.OrganizationMapper;
import com.ebikes.organizations.services.documents.DocumentService;
import com.ebikes.organizations.services.images.ImageService;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationRequestFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("OrganizationController")
@WebMvcTest(OrganizationController.class)
class OrganizationControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private DocumentService documentService;

  @MockitoBean private ImageService imageService;

  @MockitoBean private OrganizationMapper organizationMapper;

  @MockitoBean private OrganizationService organizationService;

  private static final UUID ORG_ID = UUID.randomUUID();

  @Nested
  @DisplayName("PUT /organizations/{id}/logo")
  class ConfirmLogoUpload {

    private ImageUploadConfirmationRequest validRequest() {
      return new ImageUploadConfirmationRequest(102400L, "test-key", "image/jpeg");
    }

    @Test
    @DisplayName("should return 204 when user is ORGANIZATION_ADMIN")
    void shouldReturn204WhenOrganizationAdmin() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}/logo", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isNoContent());

      verify(imageService).confirmImageUpload(eq(ORG_ID), any());
    }

    @Test
    @DisplayName("should return 204 when user is SYSTEM_ADMIN")
    void shouldReturn204WhenSystemAdmin() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}/logo", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}/logo", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}/logo", ORG_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}/logo", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(validRequest())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("POST /organizations")
  class Create {

    @Test
    @DisplayName("should return 201 when user is CUSTOMER")
    void shouldReturn201WhenCustomer() throws Exception {
      when(organizationService.create(any())).thenReturn(OrganizationFixtures.pendingApproval());

      mockMvc
          .perform(
              post("/organizations")
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.create())))
          .andExpect(status().isCreated());

      verify(organizationService).create(any());
    }

    @Test
    @DisplayName("should return 201 when user is SYSTEM_ADMIN")
    void shouldReturn201WhenSystemAdmin() throws Exception {
      when(organizationService.create(any())).thenReturn(OrganizationFixtures.pendingApproval());

      mockMvc
          .perform(
              post("/organizations")
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.create())))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              post("/organizations")
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              post("/organizations")
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.create())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              post("/organizations")
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.create())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /organizations/{id}/deactivate")
  class Deactivate {

    @Test
    @DisplayName("should return 200 when user is SYSTEM_ADMIN")
    void shouldReturn200WhenSystemAdmin() throws Exception {
      when(organizationService.deactivate(any(), any()))
          .thenReturn(OrganizationFixtures.deactivated("Compliance issue"));

      mockMvc
          .perform(
              patch("/organizations/{id}/deactivate", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(OrganizationRequestFixtures.deactivate())))
          .andExpect(status().isOk());

      verify(organizationService).deactivate(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch("/organizations/{id}/deactivate", ORG_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(OrganizationRequestFixtures.deactivate())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              patch("/organizations/{id}/deactivate", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(
                      objectMapper.writeValueAsString(OrganizationRequestFixtures.deactivate())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{id}")
  class FindById {

    @Test
    @DisplayName("should return 200 when authenticated")
    void shouldReturn200WhenAuthenticated() throws Exception {
      when(organizationService.findById(any())).thenReturn(OrganizationFixtures.active());

      mockMvc
          .perform(get("/organizations/{id}", ORG_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(organizationService).findById(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/organizations/{id}", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{id}/documents/previews")
  class FindDocumentPreviews {

    @Test
    @DisplayName("should return 200 with includeInactive defaulting to false")
    void shouldReturn200WithIncludeInactiveDefaultingToFalse() throws Exception {
      when(documentService.findByOrganizationWithPreviews(any(), any(Boolean.class)))
          .thenReturn(List.of());

      mockMvc
          .perform(get("/organizations/{id}/documents/previews", ORG_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByOrganizationWithPreviews(any(), eq(false));
    }

    @Test
    @DisplayName("should return 200 when includeInactive is true")
    void shouldReturn200WhenIncludeInactiveIsTrue() throws Exception {
      when(documentService.findByOrganizationWithPreviews(any(), any(Boolean.class)))
          .thenReturn(List.of());

      mockMvc
          .perform(
              get("/organizations/{id}/documents/previews", ORG_ID)
                  .param("includeInactive", "true")
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByOrganizationWithPreviews(any(), eq(true));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/organizations/{id}/documents/previews", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{id}/documents")
  class FindDocuments {

    @Test
    @DisplayName("should return 200 with includeInactive defaulting to false")
    void shouldReturn200WithIncludeInactiveDefaultingToFalse() throws Exception {
      when(documentService.findByOrganization(any(), any(Boolean.class))).thenReturn(List.of());

      mockMvc
          .perform(get("/organizations/{id}/documents", ORG_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByOrganization(any(), eq(false));
    }

    @Test
    @DisplayName("should return 200 when includeInactive is true")
    void shouldReturn200WhenIncludeInactiveIsTrue() throws Exception {
      when(documentService.findByOrganization(any(), any(Boolean.class))).thenReturn(List.of());

      mockMvc
          .perform(
              get("/organizations/{id}/documents", ORG_ID)
                  .param("includeInactive", "true")
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(documentService).findByOrganization(any(), eq(true));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/organizations/{id}/documents", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/reference")
  class FindReferences {

    @Test
    @DisplayName("should return 200 when authenticated")
    void shouldReturn200WhenAuthenticated() throws Exception {
      when(organizationService.findReferencesByIds(any())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/organizations/reference")
                  .param("ids", UUID.randomUUID().toString())
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(organizationService).findReferencesByIds(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get("/organizations/reference")
                  .param("ids", UUID.randomUUID().toString())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("POST /organizations/{id}/logo/upload-url")
  class GenerateLogoUploadUrl {

    @Test
    @DisplayName("should return 200 when user is ORGANIZATION_ADMIN")
    void shouldReturn200WhenOrganizationAdmin() throws Exception {
      ImageUploadInitiationResponse response =
          new ImageUploadInitiationResponse(
              Instant.now().plusSeconds(3600),
              "test-key",
              Map.of(),
              "https://s3.example.com/upload");
      when(imageService.generateImageUploadUrl(ORG_ID)).thenReturn(response);

      mockMvc
          .perform(
              post("/organizations/{id}/logo/upload-url", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(imageService).generateImageUploadUrl(ORG_ID);
    }

    @Test
    @DisplayName("should return 200 when user is SYSTEM_ADMIN")
    void shouldReturn200WhenSystemAdmin() throws Exception {
      ImageUploadInitiationResponse uploadUrlData =
          new ImageUploadInitiationResponse(
              Instant.now().plusSeconds(3600),
              "test-key",
              Map.of(),
              "https://s3.example.com/upload");
      when(imageService.generateImageUploadUrl(ORG_ID)).thenReturn(uploadUrlData);

      mockMvc
          .perform(
              post("/organizations/{id}/logo/upload-url", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(post("/organizations/{id}/logo/upload-url", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              post("/organizations/{id}/logo/upload-url", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{id}/logo")
  class GetLogo {

    @Test
    @DisplayName("should return 302 with Location header when authenticated")
    void shouldReturn302WithLocationHeaderWhenAuthenticated() throws Exception {
      String redirectUrl = "https://s3.example.com/logos/presigned";
      when(imageService.generateImageRedirectUrl(ORG_ID)).thenReturn(redirectUrl);

      mockMvc
          .perform(get("/organizations/{id}/logo", ORG_ID).with(authenticatedJwt()))
          .andExpect(status().isFound())
          .andExpect(header().string(HttpHeaders.LOCATION, redirectUrl));

      verify(imageService).generateImageRedirectUrl(ORG_ID);
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/organizations/{id}/logo", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations")
  class Search {

    @Test
    @DisplayName("should return 200 when authenticated")
    void shouldReturn200WhenAuthenticated() throws Exception {
      when(organizationService.search(any())).thenReturn(Page.empty());

      mockMvc.perform(get("/organizations").with(authenticatedJwt())).andExpect(status().isOk());

      verify(organizationService).search(any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc.perform(get("/organizations").with(anonymous())).andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PUT /organizations/{id}")
  class Update {

    @Test
    @DisplayName("should return 204 when user is ORGANIZATION_ADMIN")
    void shouldReturn204WhenOrganizationAdmin() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.update())))
          .andExpect(status().isNoContent());

      verify(organizationService).update(any(), any());
    }

    @Test
    @DisplayName("should return 204 when user is CUSTOMER")
    void shouldReturn204WhenCustomer() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.update())))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}", ORG_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.update())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{id}", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.BRANCH_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(OrganizationRequestFixtures.update())))
          .andExpect(status().isForbidden());
    }
  }
}
