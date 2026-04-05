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
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnapshotCreator {

  private static final Set<String> EXCLUDED_FIELDS =
      Set.of("class", "id", "createdAt", "createdBy", "updatedAt", "updatedBy", "version");

  private final ObjectMapper objectMapper;

  public List<FieldChange> extractChanges(Object request, Object existing) {
    if (request == null || existing == null) {
      throw new IllegalArgumentException("Request and existing object cannot be null");
    }

    BeanWrapper requestWrapper = new BeanWrapperImpl(request);
    BeanWrapper existingWrapper = new BeanWrapperImpl(existing);
    PropertyDescriptor[] descriptors = requestWrapper.getPropertyDescriptors();
    List<FieldChange> changes = new ArrayList<>(descriptors.length);

    for (PropertyDescriptor descriptor : descriptors) {
      String fieldName = descriptor.getName();

      try {
        Object requestValue = requestWrapper.getPropertyValue(fieldName);

        if (EXCLUDED_FIELDS.contains(fieldName) || requestValue == null) {
          continue;
        }

        Object existingValue =
            existingWrapper.isReadableProperty(fieldName)
                ? existingWrapper.getPropertyValue(fieldName)
                : null;

        String serializedNew = serializeValue(requestValue);
        String serializedOld = existingValue != null ? serializeValue(existingValue) : null;

        if (!serializedNew.equals(serializedOld)) {
          FieldType fieldType = FieldTypeDetector.detectFieldType(requestValue);
          changes.add(new FieldChange(fieldName, fieldType, serializedNew, serializedOld));
        }
      } catch (Exception e) {
        log.warn(
            "Failed to extract change for field '{}' from {}: {}",
            fieldName,
            request.getClass().getSimpleName(),
            e.getMessage());
      }
    }

    return changes;
  }

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
