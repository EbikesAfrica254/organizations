package com.ebikes.organizations.support.changes;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.Entity;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.FieldType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotCreator {

  private static final Set<String> EXCLUDED_FIELDS =
      Set.of("class", "id", "createdAt", "createdBy", "updatedAt", "updatedBy", "version");

  private final tools.jackson.databind.ObjectMapper objectMapper;

  public List<FieldChange> extractFields(Object entity) {
    if (entity == null) {
      throw new IllegalArgumentException("Entity cannot be null");
    }

    BeanWrapper wrapper = new BeanWrapperImpl(entity);
    PropertyDescriptor[] descriptors = wrapper.getPropertyDescriptors();
    List<FieldChange> fieldChanges = new ArrayList<>(descriptors.length);

    for (PropertyDescriptor descriptor : descriptors) {
      String fieldName = descriptor.getName();

      if (EXCLUDED_FIELDS.contains(fieldName)) {
        continue;
      }

      try {
        Object value = wrapper.getPropertyValue(fieldName);
        if (value != null) {
          FieldType fieldType = FieldTypeDetector.detectFieldType(value);
          String serializedValue = serializeValue(value);
          fieldChanges.add(new FieldChange(fieldName, fieldType, serializedValue, null));
        }
      } catch (Exception e) {
        log.warn(
            "Failed to extract field '{}' from {}: {}",
            fieldName,
            entity.getClass().getSimpleName(),
            e.getMessage());
      }
    }

    return fieldChanges;
  }

  private UUID extractId(Object entity) {
    try {
      BeanWrapper wrapper = new BeanWrapperImpl(entity);
      Object id = wrapper.getPropertyValue("id");
      return id instanceof UUID uuid ? uuid : null;
    } catch (Exception e) {
      log.debug(
          "Could not extract ID from {}: {}", entity.getClass().getSimpleName(), e.getMessage());
      return null;
    }
  }

  private boolean isJpaEntity(Object obj) {
    if (obj == null) {
      return false;
    }

    Class<?> clazz = obj.getClass();

    if (clazz.getName().contains("$HibernateProxy")) {
      clazz = clazz.getSuperclass();
    }

    return clazz.isAnnotationPresent(Entity.class);
  }

  private boolean isSimpleType(Object value) {
    return value instanceof String
        || value instanceof Number
        || value instanceof Boolean
        || value instanceof Enum<?>
        || value instanceof java.time.temporal.Temporal
        || value instanceof java.util.Date;
  }

  private String serializeValue(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Collection<?> collection) {
      return serializeCollection(collection);
    }

    if (isJpaEntity(value)) {
      UUID id = extractId(value);
      // Single entity stored as an array for consistency with collections
      return id != null ? objectMapper.writeValueAsString(List.of(id)) : "[]";
    }

    if (isSimpleType(value)) {
      return value.toString();
    }

    return objectMapper.writeValueAsString(value);
  }

  private String serializeCollection(Collection<?> collection) {
    if (collection.isEmpty()) {
      return "[]";
    }

    Object firstElement = collection.iterator().next();

    if (isJpaEntity(firstElement)) {
      List<UUID> ids = new ArrayList<>(collection.size());
      for (Object item : collection) {
        UUID id = extractId(item);
        if (id != null) {
          ids.add(id);
        }
      }
      return objectMapper.writeValueAsString(ids);
    }

    return objectMapper.writeValueAsString(collection);
  }
}
