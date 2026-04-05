package com.ebikes.organizations.support.changes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.FieldType;
import com.ebikes.organizations.enums.OrganizationStatus;
import com.ebikes.organizations.support.changes.ChangeApplier.ChangeApplicationException;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@DisplayName("ChangeApplier")
class ChangeApplierTest {

  private ChangeApplier service;

  @BeforeEach
  void setUp() {
    service = new ChangeApplier(new ChangeValueConverter());
  }

  @Getter
  @Setter
  static class MutableEntity {

    private String name;
    private OrganizationStatus status;

    @Getter(AccessLevel.NONE)
    private final String readOnly = "fixed";

    public String getReadOnly() {
      return readOnly;
    }
  }

  @Nested
  @DisplayName("applyChanges")
  class ApplyChanges {

    @Test
    @DisplayName("should apply a string field change to the entity")
    void shouldApplyStringFieldChange() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(new FieldChange("name", FieldType.STRING, "New Name", "Old Name"));

      service.applyChanges(entity, changes);

      assertThat(entity.getName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("should apply an enum field change to the entity")
    void shouldApplyEnumFieldChange() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(
              new FieldChange("status", FieldType.ENUM, OrganizationStatus.ACTIVE.name(), null));

      service.applyChanges(entity, changes);

      assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("should apply multiple changes in sequence")
    void shouldApplyMultipleChanges() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(
              new FieldChange("name", FieldType.STRING, "New Name", null),
              new FieldChange(
                  "status", FieldType.ENUM, OrganizationStatus.DEACTIVATED.name(), null));

      service.applyChanges(entity, changes);

      assertThat(entity.getName()).isEqualTo("New Name");
      assertThat(entity.getStatus()).isEqualTo(OrganizationStatus.DEACTIVATED);
    }

    @Test
    @DisplayName("should skip field silently when no matching property descriptor found")
    void shouldSkipWhenFieldNotFound() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(new FieldChange("nonExistentField", FieldType.STRING, "value", null));

      service.applyChanges(entity, changes);

      // no exception thrown, entity unchanged
      assertThat(entity.getName()).isNull();
    }

    @Test
    @DisplayName("should skip field silently when field has no setter")
    void shouldSkipReadOnlyField() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(new FieldChange("readOnly", FieldType.STRING, "attempted", null));

      service.applyChanges(entity, changes);

      assertThat(entity.getReadOnly()).isEqualTo("fixed");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when entity is null")
    void shouldThrowWhenEntityNull() {
      List<FieldChange> changes = List.of();
      assertThatThrownBy(() -> service.applyChanges(null, changes))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when changes list is null")
    void shouldThrowWhenChangesNull() {
      MutableEntity entity = new MutableEntity();
      assertThatThrownBy(() -> service.applyChanges(entity, null))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw ChangeApplicationException when value conversion fails")
    void shouldThrowWhenConversionFails() {
      MutableEntity entity = new MutableEntity();
      List<FieldChange> changes =
          List.of(new FieldChange("status", FieldType.ENUM, "NOT_A_VALID_STATUS", null));

      assertThatThrownBy(() -> service.applyChanges(entity, changes))
          .isInstanceOf(ChangeApplicationException.class);
    }
  }
}
