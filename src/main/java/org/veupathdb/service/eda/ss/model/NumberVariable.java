package org.veupathdb.service.eda.ss.model;

import java.util.List;

public class NumberVariable extends VariableWithValues {

  private final String unitsId;
  private final String scaleId;
  private final Integer precision;
  private final Number displayRangeMin;
  private final Number displayRangeMax;
  private final Number rangeMin;
  private final Number rangeMax;
  private final Number binWidthOverride;
  private final Number binWidth;

  public NumberVariable(
      String providerLabel, String id, Entity entity, boolean isLongitude, VariableDataShape dataShape,
      VariableDisplayType displayType, Integer displayOrder, String unitsId, String scaleId, Integer precision,
      String displayName, String parentId, String definition, List<String> vocabulary, Number displayRangeMin,
      Number displayRangeMax, Number rangeMin, Number rangeMax, Number binWidthOverride,
      Number binWidth, Boolean isTemporal, Boolean isFeatured, Boolean isMergeKey,
      Number distinctValuesCount, Boolean isMultiValued) {
    
    super(providerLabel, id, entity, isLongitude? VariableType.LONGITUDE : VariableType.NUMBER,
        dataShape, displayType, displayName, displayOrder, parentId, definition,
        vocabulary, isTemporal, isFeatured, isMergeKey, distinctValuesCount, isMultiValued);

    String errPrefix = "In entity " + entity.getId() + " variable " + id + " has a null ";

    if (unitsId == null)
      throw new RuntimeException(errPrefix + "units");
    if (precision == null)
      throw new RuntimeException(errPrefix + "precision");

    this.unitsId = unitsId;
    this.scaleId = scaleId;
    this.precision = precision;
    this.displayRangeMin = displayRangeMin;
    this.displayRangeMax = displayRangeMax;
    this.rangeMin = rangeMin;
    this.rangeMax = rangeMax;
    this.binWidthOverride = binWidthOverride;
    this.binWidth = binWidth;
  }

  public String getUnitsId() {
    return unitsId;
  }

  public String getScaleId() {
    return scaleId;
  }

  public Integer getPrecision() {
    return precision;
  }

  public Number getDisplayRangeMin() {
    return displayRangeMin;
  }

  public Number getDisplayRangeMax() {
    return displayRangeMax;
  }

  public Number getRangeMin() {
    return rangeMin;
  }

  public Number getRangeMax() {
    return rangeMax;
  }

  public Number getBinWidthOverride() {
    return binWidthOverride;
  }

  public Number getBinWidth() {
    return binWidth;
  }

}
