package com.ebikes.organizations.support.changes;

import java.util.ArrayList;
import java.util.List;

import org.javers.core.Javers;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.diff.changetype.container.ContainerElementChange;
import org.javers.core.diff.changetype.container.ElementValueChange;
import org.javers.core.diff.changetype.container.ListChange;
import org.javers.core.diff.changetype.container.ValueAdded;
import org.javers.core.diff.changetype.container.ValueRemoved;
import org.springframework.stereotype.Component;

import com.ebikes.organizations.dtos.internal.FieldChange;
import com.ebikes.organizations.enums.FieldType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChangeDetector {

  private final Javers javers;

  public List<FieldChange> detectChanges(Object before, Object after) {
    validateInputs(before, after);

    Diff diff = javers.compare(before, after);

    if (!diff.hasChanges()) {
      return List.of();
    }

    return convertToFieldChanges(diff.getChanges());
  }

  private List<FieldChange> convertListChange(ListChange listChange) {
    String baseFieldName = listChange.getPropertyName();
    List<ContainerElementChange> elementChanges = listChange.getChanges();
    List<FieldChange> fieldChanges = new ArrayList<>(elementChanges.size());

    for (ContainerElementChange elementChange : elementChanges) {
      Integer index = elementChange.getIndex();
      String fieldName = String.format("%s[%d]", baseFieldName, index);

      FieldChange fieldChange =
          switch (elementChange) {
            case ValueAdded va -> createFieldChange(fieldName, va.getAddedValue().toString(), null);
            case ValueRemoved vr ->
                createFieldChange(fieldName, null, vr.getRemovedValue().toString());
            case ElementValueChange evc ->
                createFieldChange(
                    fieldName, evc.getRightValue().toString(), evc.getLeftValue().toString());
            default -> {
              log.warn("Unknown ContainerElementChange type: {}", elementChange.getClass());
              yield null;
            }
          };

      if (fieldChange != null) {
        fieldChanges.add(fieldChange);
      }
    }

    return fieldChanges;
  }

  private List<FieldChange> convertToFieldChanges(List<Change> changes) {
    List<FieldChange> fieldChanges = new ArrayList<>(changes.size());

    for (Change change : changes) {
      if (change instanceof ValueChange valueChange) {
        fieldChanges.add(convertValueChange(valueChange));
      } else if (change instanceof ListChange listChange) {
        fieldChanges.addAll(convertListChange(listChange));
      }
    }

    return fieldChanges;
  }

  private FieldChange convertValueChange(ValueChange change) {
    String fieldName = change.getPropertyName();
    String newValue = change.getRight().toString();
    String oldValue = change.getLeft().toString();
    FieldType fieldType = FieldTypeDetector.detectFieldType(newValue);

    return new FieldChange(fieldName, fieldType, newValue, oldValue);
  }

  private FieldChange createFieldChange(String fieldName, String newValue, String oldValue) {
    Object valueForType = newValue != null ? newValue : oldValue;
    FieldType fieldType = FieldTypeDetector.detectFieldType(valueForType);
    return new FieldChange(fieldName, fieldType, newValue, oldValue);
  }

  private void validateInputs(Object before, Object after) {
    if (before == null && after == null) {
      throw new IllegalArgumentException("Both before and after objects cannot be null");
    }

    if (before == null || after == null) {
      throw new IllegalArgumentException("Only one object is null - invalid comparison");
    }

    if (!before.getClass().equals(after.getClass())) {
      throw new IllegalArgumentException(
          String.format(
              "Objects must be of the same type. Before: %s, After: %s",
              before.getClass().getSimpleName(), after.getClass().getSimpleName()));
    }
  }
}
