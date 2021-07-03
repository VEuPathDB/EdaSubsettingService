package org.veupathdb.service.eda.ss.model;

import java.util.List;


public class VariableWithValues extends Variable {

  private final List<String> vocabulary;
  private final Boolean isTemporal;
  private final Boolean isFeatured;
  private final Boolean isMergeKey;
  private final Number distinctValuesCount;
  private final Boolean isMultiValued;

  public VariableWithValues(String providerLabel, String id, Entity entity, VariableType type,
      VariableDataShape dataShape, VariableDisplayType displayType, String displayName, Integer displayOrder,
      String parentId, String definition, List<String> vocabulary, Boolean isTemporal, Boolean isFeatured,
      Boolean isMergeKey, Number distinctValuesCount, Boolean isMultiValued) {

    super(providerLabel, id, entity, type, dataShape, displayType, displayName, displayOrder, parentId, definition);

    this.vocabulary = vocabulary;
    this.distinctValuesCount = distinctValuesCount;
    this.isFeatured = isFeatured;
    this.isTemporal = isTemporal;
    this.isMergeKey = isMergeKey;
    this.isMultiValued = isMultiValued;
  }

  public List<String> getVocabulary() {
    return vocabulary;
  }

  public Boolean getIsTemporal() {
    return isTemporal;
  }

  public Boolean getIsFeatured() {
    return isFeatured;
  }

  public Boolean getIsMergeKey() {
    return isMergeKey;
  }

  public Number getDistinctValuesCount() {
    return distinctValuesCount;
  }

  public Boolean getIsMultiValued() {
    return isMultiValued;
  }

}
