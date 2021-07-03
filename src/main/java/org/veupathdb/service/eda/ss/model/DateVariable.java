package org.veupathdb.service.eda.ss.model;

import java.util.List;

public class DateVariable extends VariableWithValues {

  private final String displayRangeMin;
  private final String displayRangeMax;
  private final String rangeMin;
  private final String rangeMax;
  private final String binWidthOverride;
  private final String binWidth;

  public DateVariable(
      String providerLabel, String id, Entity entity, VariableDataShape dataShape,
      VariableDisplayType displayType, String displayName, Integer displayOrder,
      String parentId, String definition, List<String> vocabulary, String displayRangeMin,
      String displayRangeMax, String rangeMin, String rangeMax, String binWidthOverride,
      String binWidth, Boolean isTemporal, Boolean isFeatured, Boolean isMergeKey, 
      Number distinctValuesCount, Boolean isMultiValued) {

    super(providerLabel, id, entity, VariableType.DATE, dataShape, displayType, displayName, displayOrder, parentId, definition,
        vocabulary, isTemporal, isFeatured, isMergeKey, distinctValuesCount, isMultiValued);

    String errPrefix = "In entity " + entity.getId() + " variable " + id + " has a null ";

    this.displayRangeMin = displayRangeMin;
    this.displayRangeMax = displayRangeMax;
    this.rangeMin = rangeMin;
    this.rangeMax = rangeMax;
    this.binWidthOverride = binWidthOverride;
    this.binWidth = binWidth;
  }

  public String getDisplayRangeMin() {
    return displayRangeMin;
  }

  public String getDisplayRangeMax() {
    return displayRangeMax;
  }

  public String getRangeMin() {
    return rangeMin;
  }

  public String getRangeMax() {
    return rangeMax;
  }

  public String getBinWidthOverride() {
    return binWidthOverride;
  }

  public String getBinWidth() {
    return binWidth;
  }
}
