package com.ebikes.organizations.support.changes;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.organizations.enums.FieldType;
import com.ebikes.organizations.enums.OrganizationStatus;

@DisplayName("FieldTypeDetector")
class FieldTypeDetectorTest {

  @Nested
  @DisplayName("detectFieldType")
  class DetectFieldType {

    @Test
    @DisplayName("should return OBJECT when value is null")
    void shouldReturnObjectWhenNull() {
      assertThat(FieldTypeDetector.detectFieldType(null)).isEqualTo(FieldType.OBJECT);
    }

    @Test
    @DisplayName("should return STRING for a String value")
    void shouldReturnStringForString() {
      assertThat(FieldTypeDetector.detectFieldType("hello")).isEqualTo(FieldType.STRING);
    }

    @Test
    @DisplayName("should return ENUM for an enum value")
    void shouldReturnEnumForEnum() {
      assertThat(FieldTypeDetector.detectFieldType(OrganizationStatus.ACTIVE))
          .isEqualTo(FieldType.ENUM);
    }

    @Test
    @DisplayName("should return DATE for a LocalDate value")
    void shouldReturnDateForLocalDate() {
      assertThat(FieldTypeDetector.detectFieldType(LocalDate.now())).isEqualTo(FieldType.DATE);
    }

    @Test
    @DisplayName("should return DATE for an OffsetDateTime value")
    void shouldReturnDateForOffsetDateTime() {
      assertThat(FieldTypeDetector.detectFieldType(OffsetDateTime.now())).isEqualTo(FieldType.DATE);
    }

    @Test
    @DisplayName("should return BOOLEAN for a Boolean value")
    void shouldReturnBooleanForBoolean() {
      assertThat(FieldTypeDetector.detectFieldType(true)).isEqualTo(FieldType.BOOLEAN);
    }

    @Test
    @DisplayName("should return NUMBER for an Integer value")
    void shouldReturnNumberForInteger() {
      assertThat(FieldTypeDetector.detectFieldType(42)).isEqualTo(FieldType.NUMBER);
    }

    @Test
    @DisplayName("should return NUMBER for a Double value")
    void shouldReturnNumberForDouble() {
      assertThat(FieldTypeDetector.detectFieldType(3.14)).isEqualTo(FieldType.NUMBER);
    }

    @Test
    @DisplayName("should return OBJECT for an unrecognised type")
    void shouldReturnObjectForUnrecognisedType() {
      assertThat(FieldTypeDetector.detectFieldType(new Object())).isEqualTo(FieldType.OBJECT);
    }
  }
}
