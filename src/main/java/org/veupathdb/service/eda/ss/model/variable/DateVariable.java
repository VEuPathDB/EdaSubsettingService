package org.veupathdb.service.eda.ss.model.variable;

import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.generated.model.BinUnits;

public class DateVariable extends VariableWithValues {

  public static class Properties {

    public final String displayRangeMin;
    public final String displayRangeMax;
    public final String rangeMin;
    public final String rangeMax;
    public final Integer binSize;
    public final BinUnits binUnits;
    public final BinUnits binUnitsOverride;

    public Properties(VariableDataShape dataShape, // needed for bin units calculations
                      String displayRangeMin, String displayRangeMax,
                      String rangeMin, String rangeMax, Integer binSize,
                      String binUnits, String binUnitsOverride) {
      this.displayRangeMin = displayRangeMin;
      this.displayRangeMax = displayRangeMax;
      this.rangeMin = rangeMin;
      this.rangeMax = rangeMax;

      // massage bin values based on data shape
      if (dataShape == VariableDataShape.CONTINUOUS) {
        this.binUnits = BinUnits.valueOf(binUnits.toUpperCase());
        this.binUnitsOverride = binUnitsOverride == null ? null : BinUnits.valueOf(binUnitsOverride.toUpperCase());
        this.binSize = binSize;
      }
      else {
        this.binUnits = null;
        this.binUnitsOverride = null;
        this.binSize = null;
      }
    }
  }

  private final Properties _properties;

  public DateVariable(Variable.Properties varProperties, VariableWithValues.Properties valueProperties, Properties properties) {
    super(varProperties, valueProperties);
    _properties = properties;
    validateType(VariableType.DATE);
  }

  public String getDisplayRangeMin() {
    return _properties.displayRangeMin;
  }

  public String getDisplayRangeMax() {
    return _properties.displayRangeMax;
  }

  public String getRangeMin() {
    return _properties.rangeMin;
  }

  public String getRangeMax() {
    return _properties.rangeMax;
  }

  public BinUnits getBinUnitsOverride() {
    return _properties.binUnitsOverride;
  }

  public BinUnits getBinUnits() {
    return _properties.binUnits;
  }

  public Integer getBinSize() {
    return _properties.binSize;
  }

  public BinUnits getDefaultBinUnits() {
    return _properties.binUnitsOverride != null
        ? _properties.binUnitsOverride
        : _properties.binUnits;
  }

  public static DateVariable assertType(Variable variable) {
    if (variable instanceof DateVariable) return (DateVariable)variable;
    throw new BadRequestException("Variable " + variable.getId() +
        " of entity " + variable.getEntityId() + " is not a date variable.");
  }
}
