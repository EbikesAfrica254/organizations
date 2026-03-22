package com.ebikes.organizations.support.changes;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChangeValueConverter {

  private static final Map<Class<?>, Function<String, Object>> SIMPLE_CONVERTERS =
      Map.ofEntries(
          Map.entry(String.class, value -> value),
          Map.entry(Integer.class, Integer::valueOf),
          Map.entry(int.class, Integer::valueOf),
          Map.entry(Long.class, Long::valueOf),
          Map.entry(long.class, Long::valueOf),
          Map.entry(Boolean.class, Boolean::valueOf),
          Map.entry(boolean.class, Boolean::valueOf),
          Map.entry(Double.class, Double::valueOf),
          Map.entry(double.class, Double::valueOf),
          Map.entry(LocalDate.class, LocalDate::parse),
          Map.entry(OffsetDateTime.class, OffsetDateTime::parse),
          Map.entry(Instant.class, Instant::parse));

  public Object convert(Object value, Class<?> targetType) {
    if (value == null) {
      return null;
    }

    if (targetType.isInstance(value)) {
      return value;
    }

    String stringValue = value.toString();

    try {
      if (targetType.isEnum()) {
        return convertEnum(stringValue, targetType);
      }

      Function<String, Object> converter = SIMPLE_CONVERTERS.get(targetType);
      if (converter != null) {
        return converter.apply(stringValue);
      }

      log.warn(
          "Unsupported type conversion: {} to {}. Returning value as-is.",
          value.getClass().getSimpleName(),
          targetType.getSimpleName());
      return value;

    } catch (Exception e) {
      throw new IllegalArgumentException(
          "Failed to convert value '%s' to type %s"
              .formatted(stringValue, targetType.getSimpleName()),
          e);
    }
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Object convertEnum(String value, Class<?> targetType) {
    return Enum.valueOf((Class<? extends Enum>) targetType.asSubclass(Enum.class), value);
  }
}
