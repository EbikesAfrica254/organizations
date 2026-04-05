package com.ebikes.organizations.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.enums.UserRole;
import com.ebikes.organizations.mappers.BranchAddressMapper;
import com.ebikes.organizations.mappers.BranchMapper;
import com.ebikes.organizations.services.branches.BranchAddressService;
import com.ebikes.organizations.services.branches.BranchService;
import com.ebikes.organizations.services.organizations.OrganizationService;
import com.ebikes.organizations.support.fixtures.BranchFixtures;
import com.ebikes.organizations.support.fixtures.BranchRequestFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.AbstractControllerTest;

import tools.jackson.databind.ObjectMapper;

@DisplayName("BranchController")
@WebMvcTest(BranchController.class)
class BranchControllerTest extends AbstractControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private BranchAddressMapper branchAddressMapper;

  @MockitoBean private BranchAddressService branchAddressService;

  @MockitoBean private BranchMapper branchMapper;

  @MockitoBean private BranchService branchService;

  @MockitoBean private OrganizationService organizationService;

  private static final UUID ORG_ID = UUID.randomUUID();
  private static final UUID BRANCH_ID = UUID.randomUUID();

  @Nested
  @DisplayName("POST /organizations/{organizationId}/branches")
  class Create {

    @Test
    @DisplayName("should return 201 when user is ORGANIZATION_ADMIN")
    void shouldReturn201WhenOrganizationAdmin() throws Exception {
      when(organizationService.findById(any())).thenReturn(OrganizationFixtures.active());
      when(branchService.create(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              post("/organizations/{organizationId}/branches", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.create())))
          .andExpect(status().isCreated());

      verify(branchService).create(any(), any());
    }

    @Test
    @DisplayName("should return 201 when user is SYSTEM_ADMIN")
    void shouldReturn201WhenSystemAdmin() throws Exception {
      when(organizationService.findById(any())).thenReturn(OrganizationFixtures.active());
      when(branchService.create(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              post("/organizations/{organizationId}/branches", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.create())))
          .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("should return 400 when request body is invalid")
    void shouldReturn400WhenRequestBodyInvalid() throws Exception {
      mockMvc
          .perform(
              post("/organizations/{organizationId}/branches", ORG_ID)
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
              post("/organizations/{organizationId}/branches", ORG_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.create())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              post("/organizations/{organizationId}/branches", ORG_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.BRANCH_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.create())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /organizations/{organizationId}/branches/{branchId}/deactivate")
  class Deactivate {

    @Test
    @DisplayName("should return 200 when user is SYSTEM_ADMIN")
    void shouldReturn200WhenSystemAdmin() throws Exception {
      when(branchService.deactivate(any(), any(), any()))
          .thenReturn(
              BranchFixtures.deactivated(OrganizationFixtures.active(), "Compliance issue"));

      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/deactivate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.deactivate())))
          .andExpect(status().isOk());

      verify(branchService).deactivate(any(), any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/deactivate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.deactivate())))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/deactivate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.deactivate())))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{organizationId}/branches/{branchId}")
  class FindById {

    @Test
    @DisplayName("should return 200 when authenticated")
    void shouldReturn200WhenAuthenticated() throws Exception {
      when(branchService.findById(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              get("/organizations/{organizationId}/branches/{branchId}", ORG_ID, BRANCH_ID)
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(branchService).findById(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get("/organizations/{organizationId}/branches/{branchId}", ORG_ID, BRANCH_ID)
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{organizationId}/branches")
  class FindByOrganization {

    @Test
    @DisplayName("should return 200 with includeInactive defaulting to false")
    void shouldReturn200WithIncludeInactiveDefaultingToFalse() throws Exception {
      when(branchService.findByOrganization(any(), any(Boolean.class))).thenReturn(List.of());

      mockMvc
          .perform(get("/organizations/{organizationId}/branches", ORG_ID).with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(branchService).findByOrganization(any(), eq(false));
    }

    @Test
    @DisplayName("should return 200 when includeInactive is true")
    void shouldReturn200WhenIncludeInactiveIsTrue() throws Exception {
      when(branchService.findByOrganization(any(), any(Boolean.class))).thenReturn(List.of());

      mockMvc
          .perform(
              get("/organizations/{organizationId}/branches", ORG_ID)
                  .param("includeInactive", "true")
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(branchService).findByOrganization(any(), eq(true));
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(get("/organizations/{organizationId}/branches", ORG_ID).with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("GET /organizations/{organizationId}/branches/reference")
  class FindReferences {

    @Test
    @DisplayName("should return 200 when authenticated")
    void shouldReturn200WhenAuthenticated() throws Exception {
      when(branchService.findReferencesByIds(any(), any())).thenReturn(List.of());

      mockMvc
          .perform(
              get("/organizations/{organizationId}/branches/reference", ORG_ID)
                  .param("ids", UUID.randomUUID().toString())
                  .with(authenticatedJwt()))
          .andExpect(status().isOk());

      verify(branchService).findReferencesByIds(any(), any());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              get("/organizations/{organizationId}/branches/reference", ORG_ID)
                  .param("ids", UUID.randomUUID().toString())
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH /organizations/{organizationId}/branches/{branchId}/reinstate")
  class Reinstate {

    @Test
    @DisplayName("should return 200 when user is ORGANIZATION_ADMIN")
    void shouldReturn200WhenOrganizationAdmin() throws Exception {
      when(branchAddressService.findBranchLocationByBranchId(any())).thenReturn(null);
      when(branchAddressMapper.toResponse(any())).thenReturn(null);
      when(branchMapper.toResponse(any(Branch.class), any())).thenReturn(null);
      when(branchService.reinstate(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/reinstate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(branchService).reinstate(any(), any());
    }

    @Test
    @DisplayName("should return 200 when user is SYSTEM_ADMIN")
    void shouldReturn200WhenSystemAdmin() throws Exception {
      when(branchService.reinstate(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/reinstate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andDo(print())
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/reinstate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/reinstate",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PATCH /organizations/{organizationId}/branches/{branchId}/suspend")
  class Suspend {

    @Test
    @DisplayName("should return 200 when user is ORGANIZATION_ADMIN")
    void shouldReturn200WhenOrganizationAdmin() throws Exception {
      when(branchService.suspend(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/suspend",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN)))
          .andExpect(status().isOk());

      verify(branchService).suspend(any(), any());
    }

    @Test
    @DisplayName("should return 200 when user is SYSTEM_ADMIN")
    void shouldReturn200WhenSystemAdmin() throws Exception {
      when(branchService.suspend(any(), any()))
          .thenReturn(BranchFixtures.active(OrganizationFixtures.active()));

      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/suspend",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.SYSTEM_ADMIN)))
          .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/suspend",
                      ORG_ID,
                      BRANCH_ID)
                  .with(anonymous()))
          .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should return 403 when role is insufficient")
    void shouldReturn403WhenRoleInsufficient() throws Exception {
      mockMvc
          .perform(
              patch(
                      "/organizations/{organizationId}/branches/{branchId}/suspend",
                      ORG_ID,
                      BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.CUSTOMER)))
          .andExpect(status().isForbidden());
    }
  }

  @Nested
  @DisplayName("PUT /organizations/{organizationId}/branches/{branchId}")
  class Update {

    @Test
    @DisplayName("should return 204 when user is ORGANIZATION_ADMIN")
    void shouldReturn204WhenOrganizationAdmin() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{organizationId}/branches/{branchId}", ORG_ID, BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.ORGANIZATION_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.update())))
          .andExpect(status().isNoContent());

      verify(branchService).update(any(), any(), any());
    }

    @Test
    @DisplayName("should return 204 when user is BRANCH_ADMIN")
    void shouldReturn204WhenBranchAdmin() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{organizationId}/branches/{branchId}", ORG_ID, BRANCH_ID)
                  .with(SecurityFixtures.authenticatedJwt(UserRole.BRANCH_ADMIN))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.update())))
          .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should return 401 when unauthenticated")
    void shouldReturn401WhenUnauthenticated() throws Exception {
      mockMvc
          .perform(
              put("/organizations/{organizationId}/branches/{branchId}", ORG_ID, BRANCH_ID)
                  .with(anonymous())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(BranchRequestFixtures.update())))
          .andExpect(status().isUnauthorized());
    }
  }
}
