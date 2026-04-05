package com.ebikes.organizations.services.branches;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
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

import com.ebikes.organizations.database.entities.Branch;
import com.ebikes.organizations.database.entities.BranchAddress;
import com.ebikes.organizations.database.models.Address;
import com.ebikes.organizations.database.repositories.BranchAddressRepository;
import com.ebikes.organizations.dtos.requests.branches.BranchAddressRequest;
import com.ebikes.organizations.enums.AddressTag;
import com.ebikes.organizations.exceptions.ResourceNotFoundException;
import com.ebikes.organizations.support.fixtures.BranchFixtures;
import com.ebikes.organizations.support.fixtures.OrganizationFixtures;

@DisplayName("BranchAddressService")
@ExtendWith(MockitoExtension.class)
class BranchAddressServiceTest {

  private static final UUID BRANCH_ID = UUID.randomUUID();

  @Mock private BranchAddressRepository branchAddressRepository;

  private BranchAddressService service;
  private Branch branch;

  @BeforeEach
  void setUp() {
    service = new BranchAddressService(branchAddressRepository);
    branch = BranchFixtures.active(OrganizationFixtures.active());
    ReflectionTestUtils.setField(branch, "id", BRANCH_ID);
  }

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should build BranchAddress with BRANCH_LOCATION tag from request and save")
    void shouldCreateBranchAddressFromRequest() {
      BranchAddressRequest request =
          new BranchAddressRequest("Nairobi", "Kenya", null, null, "00100", "123 Main St");
      BranchAddress saved =
          BranchAddress.builder()
              .addressTag(AddressTag.BRANCH_LOCATION)
              .branch(branch)
              .city(request.city())
              .country(request.country())
              .postalCode(request.postalCode())
              .streetAddress(request.streetAddress())
              .build();
      when(branchAddressRepository.save(any(BranchAddress.class))).thenReturn(saved);

      service.create(branch, request);

      verify(branchAddressRepository).save(any(BranchAddress.class));
    }
  }

  @Nested
  @DisplayName("createFromOrganizationAddress")
  class CreateFromOrganizationAddress {

    @Test
    @DisplayName("should map Address fields to BranchAddress with BRANCH_LOCATION tag and save")
    void shouldCreateBranchAddressFromOrganizationAddress() {
      Address address =
          new Address(
              AddressTag.PRIMARY,
              "Nairobi",
              "Kenya",
              new BigDecimal("-1.286389"),
              new BigDecimal("36.817223"),
              "00100",
              "123 Main St");
      BranchAddress saved =
          BranchAddress.builder()
              .addressTag(AddressTag.BRANCH_LOCATION)
              .branch(branch)
              .city(address.city())
              .country(address.country())
              .latitude(address.latitude())
              .longitude(address.longitude())
              .postalCode(address.postalCode())
              .streetAddress(address.streetAddress())
              .build();
      when(branchAddressRepository.save(any(BranchAddress.class))).thenReturn(saved);

      service.createFromOrganizationAddress(branch, address);

      verify(branchAddressRepository).save(any(BranchAddress.class));
    }
  }

  @Nested
  @DisplayName("findBranchLocationByBranchId")
  class FindBranchLocationByBranchId {

    @Test
    @DisplayName("should return address when found")
    void shouldReturnAddressWhenFound() {
      BranchAddress branchAddress =
          BranchAddress.builder()
              .addressTag(AddressTag.BRANCH_LOCATION)
              .branch(branch)
              .city("Nairobi")
              .country("Kenya")
              .streetAddress("123 Main St")
              .build();
      when(branchAddressRepository.findByBranchIdAndAddressTag(
              BRANCH_ID, AddressTag.BRANCH_LOCATION))
          .thenReturn(Optional.of(branchAddress));

      BranchAddress result = service.findBranchLocationByBranchId(BRANCH_ID);

      assertThat(result).isEqualTo(branchAddress);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when not found")
    void shouldThrowWhenNotFound() {
      when(branchAddressRepository.findByBranchIdAndAddressTag(
              BRANCH_ID, AddressTag.BRANCH_LOCATION))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.findBranchLocationByBranchId(BRANCH_ID))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  @Nested
  @DisplayName("update")
  class Update {

    @Test
    @DisplayName("should find existing address, call update, and save")
    void shouldUpdateBranchAddress() {
      BranchAddressRequest request =
          new BranchAddressRequest("Mombasa", "Kenya", null, null, "80100", "456 New St");
      BranchAddress branchAddress =
          BranchAddress.builder()
              .addressTag(AddressTag.BRANCH_LOCATION)
              .branch(branch)
              .city("Nairobi")
              .country("Kenya")
              .streetAddress("123 Main St")
              .build();
      when(branchAddressRepository.findByBranchIdAndAddressTag(
              BRANCH_ID, AddressTag.BRANCH_LOCATION))
          .thenReturn(Optional.of(branchAddress));
      when(branchAddressRepository.save(any(BranchAddress.class))).thenReturn(branchAddress);

      service.update(BRANCH_ID, request);

      verify(branchAddressRepository).save(branchAddress);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when address not found")
    void shouldThrowWhenAddressNotFound() {
      BranchAddressRequest request =
          new BranchAddressRequest("Mombasa", "Kenya", null, null, "80100", "456 New St");
      when(branchAddressRepository.findByBranchIdAndAddressTag(
              BRANCH_ID, AddressTag.BRANCH_LOCATION))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.update(BRANCH_ID, request))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
