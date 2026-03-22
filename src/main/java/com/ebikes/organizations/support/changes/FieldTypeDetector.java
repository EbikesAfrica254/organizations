package com.ebikes.organizations.support.changes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import com.ebikes.organizations.enums.FieldType;

public final class FieldTypeDetector {

  private FieldTypeDetector() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static FieldType detectFieldType(Object value) {
    if (value == null) {
      return FieldType.OBJECT;
    }

    return switch (value) {
      case String ignored -> FieldType.STRING;
      case Enum<?> ignored -> FieldType.ENUM;
      case LocalDate ignored -> FieldType.DATE;
      case OffsetDateTime ignored -> FieldType.DATE;
      case Boolean ignored -> FieldType.BOOLEAN;
      case Number ignored -> FieldType.NUMBER;
      default -> FieldType.OBJECT;
    };
  }
}
