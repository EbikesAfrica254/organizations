package com.ebikes.organizations.support.changes;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.dtos.internal.FieldChange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class ChangeApplier {

  private final ChangeValueConverter changeValueConverter;

  public void applyChanges(Object entity, List<FieldChange> changes) {
    validateInputs(entity, changes);

    for (FieldChange change : changes) {
      try {
        applyFieldChange(entity, change);
      } catch (Exception e) {
        log.error(
            "Failed to apply change to field '{}': {}", change.fieldName(), e.getMessage(), e);
        throw new ChangeApplicationException(
            "Failed to apply change to field '%s' on entity %s"
                .formatted(change.fieldName(), entity.getClass().getSimpleName()),
            e);
      }
    }
  }

  private void applyFieldChange(Object entity, FieldChange change) throws Exception {
    String fieldName = change.fieldName();
    PropertyDescriptor propertyDescriptor = findPropertyDescriptor(entity, fieldName);

    if (propertyDescriptor == null) {
      log.warn(
          "No setter found for field '{}' on entity {}. Skipping.",
          fieldName,
          entity.getClass().getSimpleName());
      return;
    }

    Method setter = propertyDescriptor.getWriteMethod();
    if (setter == null) {
      log.warn(
          "Field '{}' is read-only (no setter) on entity {}. Skipping.",
          fieldName,
          entity.getClass().getSimpleName());
      return;
    }

    Object convertedValue =
        changeValueConverter.convert(change.newValue(), propertyDescriptor.getPropertyType());

    setter.invoke(entity, convertedValue);

    log.debug(
        "Applied change: field='{}', value='{}' on entity {}",
        fieldName,
        convertedValue,
        entity.getClass().getSimpleName());
  }

  private PropertyDescriptor findPropertyDescriptor(Object entity, String fieldName) {
    return Arrays.stream(BeanUtils.getPropertyDescriptors(entity.getClass()))
        .filter(pd -> pd.getName().equals(fieldName))
        .findFirst()
        .orElse(null);
  }

  private void validateInputs(Object entity, List<FieldChange> changes) {
    if (entity == null) {
      throw new IllegalArgumentException("Entity cannot be null");
    }

    if (changes == null) {
      throw new IllegalArgumentException("Changes list cannot be null");
    }
  }

  public static class ChangeApplicationException extends RuntimeException {
    public ChangeApplicationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
