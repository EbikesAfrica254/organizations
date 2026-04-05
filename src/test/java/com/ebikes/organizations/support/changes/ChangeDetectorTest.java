package com.ebikes.organizations.support.changes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.javers.core.MappingStyle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.OrganizationStatus;

@DisplayName("ChangeDetector")
class ChangeDetectorTest {

  private ChangeDetector service;

  @BeforeEach
  void setUp() {
    Javers javers = JaversBuilder.javers().withMappingStyle(MappingStyle.FIELD).build();
    service = new ChangeDetector(javers);
  }

  record SimpleEntity(String name, OrganizationStatus status, List<String> tags) {}

  record OtherEntity(String name) {}

  @Nested
  @DisplayName("detectChanges")
  class DetectChanges {

    @Test
    @DisplayName("should return empty list when objects are identical")
    void shouldReturnEmptyWhenIdentical() {
      SimpleEntity before = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("a"));
      SimpleEntity after = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("a"));

      assertThat(service.detectChanges(before, after)).isEmpty();
    }

    @Test
    @DisplayName("should detect a single string value change")
    void shouldDetectStringValueChange() {
      SimpleEntity before = new SimpleEntity("Old Name", OrganizationStatus.ACTIVE, List.of());
      SimpleEntity after = new SimpleEntity("New Name", OrganizationStatus.ACTIVE, List.of());

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(1);
      assertThat(changes.getFirst().fieldName()).isEqualTo("name");
      assertThat(changes.getFirst().newValue()).isEqualTo("New Name");
      assertThat(changes.getFirst().oldValue()).isEqualTo("Old Name");
    }

    @Test
    @DisplayName("should detect an enum value change")
    void shouldDetectEnumValueChange() {
      SimpleEntity before = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of());
      SimpleEntity after = new SimpleEntity("Acme", OrganizationStatus.DEACTIVATED, List.of());

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(1);
      assertThat(changes.getFirst().fieldName()).isEqualTo("status");
      assertThat(changes.getFirst().newValue()).isEqualTo(OrganizationStatus.DEACTIVATED.name());
      assertThat(changes.getFirst().oldValue()).isEqualTo(OrganizationStatus.ACTIVE.name());
    }

    @Test
    @DisplayName("should detect multiple value changes across fields")
    void shouldDetectMultipleValueChanges() {
      SimpleEntity before = new SimpleEntity("Old Name", OrganizationStatus.ACTIVE, List.of());
      SimpleEntity after = new SimpleEntity("New Name", OrganizationStatus.DEACTIVATED, List.of());

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(2);
      assertThat(changes)
          .extracting(FieldChange::fieldName)
          .containsExactlyInAnyOrder("name", "status");
    }

    @Test
    @DisplayName("should detect a list element added")
    void shouldDetectListElementAdded() {
      SimpleEntity before = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of());
      SimpleEntity after = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("new-tag"));

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(1);
      assertThat(changes.getFirst().fieldName()).isEqualTo("tags[0]");
      assertThat(changes.getFirst().newValue()).isEqualTo("new-tag");
      assertThat(changes.getFirst().oldValue()).isNull();
    }

    @Test
    @DisplayName("should detect a list element removed")
    void shouldDetectListElementRemoved() {
      SimpleEntity before = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("old-tag"));
      SimpleEntity after = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of());

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(1);
      assertThat(changes.getFirst().fieldName()).isEqualTo("tags[0]");
      assertThat(changes.getFirst().newValue()).isNull();
      assertThat(changes.getFirst().oldValue()).isEqualTo("old-tag");
    }

    @Test
    @DisplayName("should detect a list element value changed")
    void shouldDetectListElementValueChanged() {
      SimpleEntity before = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("old-tag"));
      SimpleEntity after = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of("new-tag"));

      List<FieldChange> changes = service.detectChanges(before, after);

      assertThat(changes).hasSize(1);
      assertThat(changes.getFirst().fieldName()).isEqualTo("tags[0]");
      assertThat(changes.getFirst().newValue()).isEqualTo("new-tag");
      assertThat(changes.getFirst().oldValue()).isEqualTo("old-tag");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when both objects are null")
    void shouldThrowWhenBothNull() {
      assertThatThrownBy(() -> service.detectChanges(null, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when only one object is null")
    void shouldThrowWhenOneNull() {
      SimpleEntity entity = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of());

      assertThatThrownBy(() -> service.detectChanges(entity, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when objects are of different types")
    void shouldThrowWhenDifferentTypes() {
      SimpleEntity entity = new SimpleEntity("Acme", OrganizationStatus.ACTIVE, List.of());
      OtherEntity other = new OtherEntity("Acme");

      assertThatThrownBy(() -> service.detectChanges(entity, other))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
