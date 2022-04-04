package org.veupathdb.service.eda.ss.model.variable;

import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.ss.model.distribution.DistributionConfig;

public class FloatingPointVariable extends NumberVariable<Double> {

  public static class Properties {

    public final String units;
    public final Long precision;

    public Properties(String units, Long precision) {
      this.units = units;
      this.precision = precision;
    }
  }

  private final Properties _properties;

  public FloatingPointVariable(
      Variable.Properties varProperties,
      VariableWithValues.Properties valueProperties,
      DistributionConfig<Double> distributionConfig,
      Properties properties) {

    super(varProperties, valueProperties, distributionConfig);
    _properties = properties;
    validateType(VariableType.NUMBER);

    String errPrefix = "In entity " + varProperties.entity.getId() + " variable " + varProperties.id + " has a null ";
    if (_properties.units == null)
      throw new RuntimeException(errPrefix + "units");
    if (_properties.precision == null)
      throw new RuntimeException(errPrefix + "precision");

  }

  @Override
  public String getUnits() {
    return _properties.units;
  }

  public Long getPrecision() {
    return _properties.precision;
  }

  @Override
  public Double validateBinWidth(Number binWidth) {
    Double doubleValue = binWidth.doubleValue();
    if (doubleValue <= 0) {
      throw new BadRequestException("binWidth must be a positive number for number variable distributions");
    }
    return doubleValue;
  }
}
