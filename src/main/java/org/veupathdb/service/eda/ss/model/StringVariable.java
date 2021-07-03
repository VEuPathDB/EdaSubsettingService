package org.veupathdb.service.eda.ss.model;

import java.util.List;

public class StringVariable extends VariableWithValues {

  public StringVariable(String providerLabel, String id, Entity entity, 
      VariableDataShape dataShape, VariableDisplayType displayType, String displayName, Integer displayOrder, String parentId,
      String definition, List<String> vocabulary, Boolean isTemporal, Boolean isFeatured, Boolean isMergeKey,
      Number distinctValuesCount, Boolean isMultiValued) {

    super(providerLabel, id, entity, VariableType.STRING, dataShape, displayType, displayName, displayOrder,
        parentId, definition, vocabulary, isTemporal, isFeatured, isMergeKey, distinctValuesCount, isMultiValued);
  }
}
