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

import lombok.Getter;
import tools.jackson.databind.ObjectMapper;

@DisplayName("SnapshotCreator")
class SnapshotCreatorTest {

  private SnapshotCreator service;

  @BeforeEach
  void setUp() {
    service = new SnapshotCreator(new ObjectMapper());
  }

  @Getter
  static class TestEntity {
    private final String id = "ignored-id";
    private final String createdAt = "ignored-createdAt";
    private final String createdBy = "ignored-createdBy";
    private final String updatedAt = "ignored-updatedAt";
    private final String updatedBy = "ignored-updatedBy";
    private final int version = 1;

    // included fields
    private final String name;
    private final OrganizationStatus status;
    private final boolean active;
    private final int count;
    private final List<String> tags;
    private final String nullField = null;

    TestEntity(
        String name, OrganizationStatus status, boolean active, int count, List<String> tags) {
      this.name = name;
      this.status = status;
      this.active = active;
      this.count = count;
      this.tags = tags;
    }
  }

  @Nested
  @DisplayName("extractChanges")
  class ExtractChanges {

    record TestRequest(String name, String nullField) {}

    @Test
    @DisplayName("should return field changes where request value differs from existing")
    void shouldReturnChangedFields() {
      TestRequest request = new TestRequest("NewName", null);
      TestEntity existing =
          new TestEntity("OldName", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractChanges(request, existing);

      assertThat(changes)
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.fieldName()).isEqualTo("name");
                assertThat(c.fieldType()).isEqualTo(FieldType.STRING);
                assertThat(c.newValue()).isEqualTo("NewName");
                assertThat(c.oldValue()).isEqualTo("OldName");
              });
    }

    @Test
    @DisplayName("should return empty when request value matches existing")
    void shouldReturnEmptyWhenNoChanges() {
      TestRequest request = new TestRequest("Acme", null);
      TestEntity existing = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractChanges(request, existing);

      assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("should skip null request fields")
    void shouldSkipNullRequestFields() {
      TestRequest request = new TestRequest(null, null);
      TestEntity existing = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractChanges(request, existing);

      assertThat(changes).isEmpty();
    }

    @Test
    @DisplayName("should skip excluded fields")
    void shouldSkipExcludedFields() {
      record RequestWithId(String id, String name) {}
      RequestWithId request = new RequestWithId("some-id", "NewName");
      TestEntity existing =
          new TestEntity("OldName", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractChanges(request, existing);

      assertThat(changes).extracting(FieldChange::fieldName).doesNotContain("id").contains("name");
    }

    @Test
    @DisplayName("should include field with oldValue null when existing does not have the property")
    void shouldIncludeFieldWhenExistingPropertyAbsent() {
      record RequestWithExtra(String extra) {}
      RequestWithExtra request = new RequestWithExtra("someValue");
      TestEntity existing = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractChanges(request, existing);

      assertThat(changes)
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.fieldName()).isEqualTo("extra");
                assertThat(c.newValue()).isEqualTo("someValue");
                assertThat(c.oldValue()).isNull();
              });
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when request is null")
    void shouldThrowWhenRequestIsNull() {
      TestEntity entity = new TestEntity("x", OrganizationStatus.ACTIVE, true, 1, List.of());
      assertThatThrownBy(() -> service.extractChanges(null, entity))
          .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when entity is null")
    void shouldThrowWhenEntityIsNull() {
      Object request = new TestRequest("name", null);
      assertThatThrownBy(() -> service.extractChanges(request, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Nested
  @DisplayName("extractFields")
  class ExtractFields {

    @Test
    @DisplayName("should return field changes for all non-null non-excluded fields")
    void shouldReturnFieldChangesForIncludedFields() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .extracting(FieldChange::fieldName)
          .contains("name", "status", "active", "count", "tags");
    }

    @Test
    @DisplayName("should exclude fields in the excluded set")
    void shouldExcludeReservedFields() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes).isNotEmpty();
      assertThat(changes)
          .extracting(FieldChange::fieldName)
          .doesNotContain(
              "id", "createdAt", "createdBy", "updatedAt", "updatedBy", "version", "class");
    }

    @Test
    @DisplayName("should skip null field values")
    void shouldSkipNullFields() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes).isNotEmpty();
      assertThat(changes).extracting(FieldChange::fieldName).doesNotContain("nullField");
    }

    @Test
    @DisplayName("should correctly detect FieldType for string field")
    void shouldDetectStringFieldType() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .filteredOn(c -> c.fieldName().equals("name"))
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.fieldType()).isEqualTo(FieldType.STRING);
                assertThat(c.newValue()).isEqualTo("Acme");
                assertThat(c.oldValue()).isNull();
              });
    }

    @Test
    @DisplayName("should correctly detect FieldType for enum field")
    void shouldDetectEnumFieldType() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .filteredOn(c -> c.fieldName().equals("status"))
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.fieldType()).isEqualTo(FieldType.ENUM);
                assertThat(c.newValue()).isEqualTo(OrganizationStatus.ACTIVE.name());
              });
    }

    @Test
    @DisplayName("should correctly detect FieldType for boolean field")
    void shouldDetectBooleanFieldType() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .filteredOn(c -> c.fieldName().equals("active"))
          .singleElement()
          .satisfies(c -> assertThat(c.fieldType()).isEqualTo(FieldType.BOOLEAN));
    }

    @Test
    @DisplayName("should correctly detect FieldType for number field")
    void shouldDetectNumberFieldType() {
      TestEntity entity = new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of());

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .filteredOn(c -> c.fieldName().equals("count"))
          .singleElement()
          .satisfies(c -> assertThat(c.fieldType()).isEqualTo(FieldType.NUMBER));
    }

    @Test
    @DisplayName("should serialize a collection field as a JSON array string")
    void shouldSerializeCollectionAsJsonArray() {
      TestEntity entity =
          new TestEntity("Acme", OrganizationStatus.ACTIVE, true, 5, List.of("tag-a", "tag-b"));

      List<FieldChange> changes = service.extractFields(entity);

      assertThat(changes)
          .filteredOn(c -> c.fieldName().equals("tags"))
          .singleElement()
          .satisfies(
              c -> {
                assertThat(c.fieldType()).isEqualTo(FieldType.OBJECT);
                assertThat(c.newValue()).isEqualTo("[\"tag-a\",\"tag-b\"]");
              });
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when entity is null")
    void shouldThrowWhenEntityNull() {
      assertThatThrownBy(() -> service.extractFields(null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
