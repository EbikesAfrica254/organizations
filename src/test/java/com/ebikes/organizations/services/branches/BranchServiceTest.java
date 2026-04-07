package com.ebikes.organizations.services.branches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.ebikes.organizations.constants.EventConstants.DomainEvents;
import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.Organization;
import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.database.repositories.BranchRepository;
import com.ebikes.organizations.dtos.requests.branches.BranchAddressRequest;
import com.ebikes.organizations.dtos.requests.branches.CreateBranchRequest;
import com.ebikes.organizations.dtos.requests.branches.UpdateBranchRequest;
import com.ebikes.organizations.dtos.responses.branches.BranchReference;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.enums.BranchStatus;
import com.ebikes.organizations.exceptions.AuthorizationException;
import com.ebikes.organizations.exceptions.DuplicateResourceException;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.exceptions.ValidationException;
import com.ebikes.organizations.mappers.BranchMapper;
import com.ebikes.organizations.services.storage.StorageService;
import com.ebikes.organizations.support.audit.AuditTemplate;
import com.ebikes.organizations.support.audit.ThrowingSupplier;
import com.ebikes.organizations.support.fixtures.BranchFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;
import com.ebikes.organizations.support.fixtures.SecurityFixtures;
import com.ebikes.organizations.support.infrastructure.WithExecutionContext;

@DisplayName("BranchService")
@ExtendWith({MockitoExtension.class, WithExecutionContext.class})
class BranchServiceTest {

  private static final UUID ORGANIZATION_ID =
      UUID.fromString(SecurityFixtures.TEST_ORGANIZATION_ID);
  private static final UUID BRANCH_ID = UUID.randomUUID();

  @Mock private AuditTemplate auditTemplate;
  @Mock private BranchAddressService branchAddressService;
  @Mock private BranchMapper branchMapper;
  @Mock private BranchRepository repository;
  @Mock private StorageService storageService;

  private BranchService service;
  private Organization organization;

  @BeforeEach
  void setUp() {
    organization = OrganizationFixtures.active();
    ReflectionTestUtils.setField(organization, "id", ORGANIZATION_ID);
    service =
        new BranchService(
            auditTemplate, branchAddressService, branchMapper, repository, storageService);
  }

  private Branch savedBranch() {
    Branch branch = BranchFixtures.active(organization);
    ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
    return branch;
  }

  @SuppressWarnings("unchecked")
  private void stubAuditExecute(Branch returned) {
    when(auditTemplate.execute(any(Branch.class), any(), any(), any(ThrowingSupplier.class)))
        .thenAnswer(
            inv -> {
              ThrowingSupplier<?, ?> supplier = inv.getArgument(3);
              supplier.get();
              return returned;
            });
  }

  @Nested
  @DisplayName("create")
  class Create {

    private CreateBranchRequest request;

    @BeforeEach
    void setUp() {
      BranchAddressRequest addressRequest =
          new BranchAddressRequest("Nairobi", "Kenya", null, null, "00100", "123 Main St");
      request =
          new CreateBranchRequest(
              addressRequest,
              "New Branch",
              "New Branch Display",
              "branch@ebikes.test",
              List.of(),
              "+254700000002");
    }

    @Test
    @DisplayName(
        "should create branch, publish audit event, delegate address creation, return saved branch")
    @SuppressWarnings("unchecked")
    void shouldCreateBranchSuccessfully() {
      Branch saved = savedBranch();
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(
              ORGANIZATION_ID, request.branchName()))
          .thenReturn(false);
      when(repository.save(any(Branch.class))).thenReturn(saved);
      stubAuditExecute(saved);

      Branch result = service.create(organization, request);

      assertThat(result).isEqualTo(saved);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.CREATED),
              any(ThrowingSupplier.class));
      verify(branchAddressService).create(saved, request.address());
    }

    @Test
    @DisplayName("should throw ValidationException when organization is not ACTIVE")
    void shouldThrowWhenOrganizationNotActive() {
      Organization pending = OrganizationFixtures.pendingApproval();
      ReflectionTestUtils.setField(pending, "id", ORGANIZATION_ID);

      assertThatThrownBy(() -> service.create(pending, request))
          .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName(
        "should throw DuplicateResourceException when branch name already exists in organization")
    void shouldThrowWhenBranchNameDuplicate() {
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(
              ORGANIZATION_ID, request.branchName()))
          .thenReturn(true);

      assertThatThrownBy(() -> service.create(organization, request))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }

  @Nested
  @DisplayName("createDefaultBranch")
  class CreateDefaultBranch {

    @Test
    @DisplayName(
        "should create branch from organization details, copy first address, publish audit event")
    @SuppressWarnings("unchecked")
    void shouldCreateDefaultBranchSuccessfully() {
      Branch saved = savedBranch();
      when(repository.save(any(Branch.class))).thenReturn(saved);
      stubAuditExecute(saved);

      // inject a minimal address list so the guard passes
      var address =
          new Address(AddressTag.PRIMARY, "Nairobi", "Kenya", null, null, "00100", "123 Main St");
      ReflectionTestUtils.setField(organization, "addresses", List.of(address));

      service.createDefaultBranch(organization);

      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.CREATED),
              any(ThrowingSupplier.class));
      verify(branchAddressService).createFromOrganizationAddress(saved, address);
    }

    @Test
    @DisplayName("should throw ValidationException when organization has no addresses")
    void shouldThrowWhenOrganizationHasNoAddresses() {
      ReflectionTestUtils.setField(organization, "addresses", List.of());

      assertThatThrownBy(() -> service.createDefaultBranch(organization))
          .isInstanceOf(ValidationException.class);
    }
  }

  @Nested
  @DisplayName("deactivate")
  class Deactivate {

    @Test
    @DisplayName("should deactivate branch, publish audit event, return saved branch")
    @SuppressWarnings("unchecked")
    void shouldDeactivateBranchSuccessfully() {
      Branch branch = BranchFixtures.active(organization);
      ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
      Branch saved = BranchFixtures.deactivated(organization, "test reason");
      ReflectionTestUtils.setField(saved, "id", BRANCH_ID);

      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.save(any(Branch.class))).thenReturn(saved);
      stubAuditExecute(saved);

      Branch result = service.deactivate(ORGANIZATION_ID, BRANCH_ID, "test reason");

      assertThat(result).isEqualTo(saved);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.DEACTIVATED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when branch not found")
    void shouldThrowWhenBranchNotFound() {
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.deactivate(ORGANIZATION_ID, BRANCH_ID, "reason"))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw AuthorizationException when branch belongs to a different organization")
    void shouldThrowWhenBranchOwnedByDifferentOrganization() {
      Organization other = OrganizationFixtures.active();
      ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
      Branch branch = BranchFixtures.active(other);
      ReflectionTestUtils.setField(branch, "id", BRANCH_ID);

      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));

      assertThatThrownBy(() -> service.deactivate(ORGANIZATION_ID, BRANCH_ID, "reason"))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("findById")
  class FindById {

    @Test
    @DisplayName("should return branch when found and ownership matches")
    void shouldReturnBranchWhenFound() {
      Branch branch = savedBranch();
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));

      Branch result = service.findById(ORGANIZATION_ID, BRANCH_ID);

      assertThat(result).isEqualTo(branch);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when branch not found")
    void shouldThrowWhenBranchNotFound() {
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findById(ORGANIZATION_ID, BRANCH_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw AuthorizationException when branch belongs to a different organization")
    void shouldThrowWhenBranchOwnedByDifferentOrganization() {
      Organization other = OrganizationFixtures.active();
      ReflectionTestUtils.setField(other, "id", UUID.randomUUID());
      Branch branch = BranchFixtures.active(other);
      ReflectionTestUtils.setField(branch, "id", BRANCH_ID);

      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));

      assertThatThrownBy(() -> service.findById(ORGANIZATION_ID, BRANCH_ID))
          .isInstanceOf(AuthorizationException.class);
    }
  }

  @Nested
  @DisplayName("findReferencesByIds")
  class FindReferencesByIds {

    @Test
    @DisplayName("should delegate to repository, map each branch, and return references")
    void shouldReturnReferences() {
      List<UUID> ids = List.of(BRANCH_ID);
      Branch active = BranchFixtures.active(organization);
      BranchReference reference = new BranchReference(BRANCH_ID, active.getDisplayName(), null);

      when(repository.findByIdInAndOrganizationId(ids, ORGANIZATION_ID))
          .thenReturn(List.of(active));
      when(branchMapper.toReference(active, null)).thenReturn(reference);

      assertThat(service.findReferencesByIds(ids, ORGANIZATION_ID)).containsExactly(reference);
    }
  }

  @Nested
  @DisplayName("findByOrganization")
  class FindByOrganization {

    @Test
    @DisplayName("should return all branches when includeInactive=true")
    void shouldReturnAllBranches() {
      Branch active = BranchFixtures.active(organization);
      Branch deactivated = BranchFixtures.deactivated(organization, "reason");
      when(repository.findByOrganizationId(ORGANIZATION_ID))
          .thenReturn(List.of(active, deactivated));

      List<Branch> result = service.findByOrganization(ORGANIZATION_ID, true);

      assertThat(result).containsExactlyInAnyOrder(active, deactivated);
    }

    @Test
    @DisplayName("should return only ACTIVE and SUSPENDED branches when includeInactive=false")
    void shouldReturnOnlyActiveBranches() {
      Branch active = BranchFixtures.active(organization);
      when(repository.findByOrganizationIdAndStatusIn(
              ORGANIZATION_ID, List.of(BranchStatus.ACTIVE, BranchStatus.SUSPENDED)))
          .thenReturn(List.of(active));

      List<Branch> result = service.findByOrganization(ORGANIZATION_ID, false);

      assertThat(result).containsExactly(active);
      verify(repository, never()).findByOrganizationId(any());
    }
  }

  @Nested
  @DisplayName("reinstate")
  class Reinstate {

    @Test
    @DisplayName("should reinstate suspended branch, publish audit event, return saved branch")
    @SuppressWarnings("unchecked")
    void shouldReinstateBranchSuccessfully() {
      Branch branch = BranchFixtures.suspended(organization);
      ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
      Branch saved = BranchFixtures.active(organization);
      ReflectionTestUtils.setField(saved, "id", BRANCH_ID);

      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.save(any(Branch.class))).thenReturn(saved);
      stubAuditExecute(saved);

      Branch result = service.reinstate(ORGANIZATION_ID, BRANCH_ID);

      assertThat(result).isEqualTo(saved);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.REINSTATED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when branch not found")
    void shouldThrowWhenBranchNotFound() {
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.reinstate(ORGANIZATION_ID, BRANCH_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("suspend")
  class Suspend {

    @Test
    @DisplayName("should suspend active branch, publish audit event, return saved branch")
    @SuppressWarnings("unchecked")
    void shouldSuspendBranchSuccessfully() {
      Branch branch = BranchFixtures.active(organization);
      ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
      Branch saved = BranchFixtures.suspended(organization);
      ReflectionTestUtils.setField(saved, "id", BRANCH_ID);

      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.save(any(Branch.class))).thenReturn(saved);
      stubAuditExecute(saved);

      Branch result = service.suspend(ORGANIZATION_ID, BRANCH_ID);

      assertThat(result).isEqualTo(saved);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.SUSPENDED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when branch not found")
    void shouldThrowWhenBranchNotFound() {
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.suspend(ORGANIZATION_ID, BRANCH_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    private UpdateBranchRequest requestWithAddress;
    private UpdateBranchRequest requestWithoutAddress;

    @BeforeEach
    void setUp() {
      BranchAddressRequest addressRequest =
          new BranchAddressRequest("Mombasa", "Kenya", null, null, "80100", "456 New St");
      requestWithAddress =
          new UpdateBranchRequest(
              addressRequest,
              "Updated Branch",
              "Updated Display",
              "updated@ebikes.test",
              List.of(),
              "+254700000003");
      requestWithoutAddress =
          new UpdateBranchRequest(
              null,
              "Updated Branch",
              "Updated Display",
              "updated@ebikes.test",
              List.of(),
              "+254700000003");
    }

    @Test
    @DisplayName("should update branch fields, publish audit event, return saved branch")
    @SuppressWarnings("unchecked")
    void shouldUpdateBranchSuccessfully() {
      Branch branch = savedBranch();
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(
              ORGANIZATION_ID, requestWithoutAddress.branchName()))
          .thenReturn(false);
      when(repository.save(any(Branch.class))).thenReturn(branch);
      stubAuditExecute(branch);

      Branch result = service.update(ORGANIZATION_ID, BRANCH_ID, requestWithoutAddress);

      assertThat(result).isEqualTo(branch);
      verify(auditTemplate)
          .execute(
              any(),
              eq(ORGANIZATION_ID.toString()),
              eq(DomainEvents.Branch.UPDATED),
              any(ThrowingSupplier.class));
    }

    @Test
    @DisplayName("should delegate to branchAddressService when address is present in request")
    void shouldUpdateAddressWhenPresent() {
      Branch branch = savedBranch();
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(any(), any()))
          .thenReturn(false);
      when(repository.save(any(Branch.class))).thenReturn(branch);
      stubAuditExecute(branch);

      service.update(ORGANIZATION_ID, BRANCH_ID, requestWithAddress);

      verify(branchAddressService).update(BRANCH_ID, requestWithAddress.address());
    }

    @Test
    @DisplayName("should skip address update when address is null in request")
    void shouldSkipAddressUpdateWhenNull() {
      Branch branch = savedBranch();
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(any(), any()))
          .thenReturn(false);
      when(repository.save(any(Branch.class))).thenReturn(branch);
      stubAuditExecute(branch);

      service.update(ORGANIZATION_ID, BRANCH_ID, requestWithoutAddress);

      verify(branchAddressService, never()).update(any(), any());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when branch not found")
    void shouldThrowWhenBranchNotFound() {
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.update(ORGANIZATION_ID, BRANCH_ID, requestWithoutAddress))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName(
        "should throw DuplicateResourceException when new branch name conflicts with another"
            + " branch")
    void shouldThrowWhenBranchNameConflicts() {
      Branch branch = savedBranch();
      when(repository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
      when(repository.existsByOrganizationIdAndBranchNameIgnoreCase(
              ORGANIZATION_ID, requestWithoutAddress.branchName()))
          .thenReturn(true);

      assertThatThrownBy(() -> service.update(ORGANIZATION_ID, BRANCH_ID, requestWithoutAddress))
          .isInstanceOf(DuplicateResourceException.class);
    }
  }
}
