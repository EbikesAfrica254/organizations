package com.ebikes.organizations.support.changes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.ebikes.organizations.enums.OrganizationStatus;

@DisplayName("ChangeValueConverter")
class ChangeValueConverterTest {

  private ChangeValueConverter service;

  @BeforeEach
  void setUp() {
    service = new ChangeValueConverter();
  }

  @Nested
  @DisplayName("convert")
  class Convert {

    @Test
    @DisplayName("should return null when value is null")
    void shouldReturnNullWhenValueNull() {
      assertThat(service.convert(null, String.class)).isNull();
    }

    @Test
    @DisplayName("should return value as-is when already the correct type")
    void shouldReturnValueWhenAlreadyCorrectType() {
      assertThat(service.convert("hello", String.class)).isEqualTo("hello");
    }

    @Test
    @DisplayName("should convert String to String")
    void shouldConvertStringToString() {
      assertThat(service.convert("hello", String.class)).isEqualTo("hello");
    }

    @Test
    @DisplayName("should convert String to Integer")
    void shouldConvertStringToInteger() {
      assertThat(service.convert("42", Integer.class)).isEqualTo(42);
    }

    @Test
    @DisplayName("should convert String to Long")
    void shouldConvertStringToLong() {
      assertThat(service.convert("100", Long.class)).isEqualTo(100L);
    }

    @Test
    @DisplayName("should convert String to Boolean")
    void shouldConvertStringToBoolean() {
      assertThat(service.convert("true", Boolean.class)).isEqualTo(true);
    }

    @Test
    @DisplayName("should convert String to Double")
    void shouldConvertStringToDouble() {
      assertThat(service.convert("3.14", Double.class)).isEqualTo(3.14);
    }

    @Test
    @DisplayName("should convert String to LocalDate")
    void shouldConvertStringToLocalDate() {
      assertThat(service.convert("2024-01-15", LocalDate.class))
          .isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    @DisplayName("should convert String to OffsetDateTime")
    void shouldConvertStringToOffsetDateTime() {
      String value = "2024-01-15T10:30:00+03:00";
      assertThat(service.convert(value, OffsetDateTime.class))
          .isEqualTo(OffsetDateTime.parse(value));
    }

    @Test
    @DisplayName("should convert String to Instant")
    void shouldConvertStringToInstant() {
      String value = "2024-01-15T10:30:00Z";
      assertThat(service.convert(value, Instant.class)).isEqualTo(Instant.parse(value));
    }

    @Test
    @DisplayName("should convert String to enum constant")
    void shouldConvertStringToEnum() {
      assertThat(service.convert("ACTIVE", OrganizationStatus.class))
          .isEqualTo(OrganizationStatus.ACTIVE);
    }

    @Test
    @DisplayName("should return value as-is when target type is unsupported")
    void shouldReturnValueAsIsForUnsupportedType() {
      Object value = new Object();
      assertThat(service.convert(value, StringBuilder.class)).isSameAs(value);
    }

    @Test
    @DisplayName("should throw IllegalArgumentException when conversion fails for a known type")
    void shouldThrowWhenConversionFails() {
      assertThatThrownBy(() -> service.convert("not-a-number", Integer.class))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}
