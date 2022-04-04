package org.veupathdb.service.eda.ss.model.variable;

import jakarta.ws.rs.BadRequestException;

public class LongitudeVariable extends VariableWithValues {

  public static class Properties {

    private final Long precision;

    public Properties(Long precision) {
      this.precision = precision;
    }
  }

  private final Properties _properties;

  public LongitudeVariable(Variable.Properties varProps, VariableWithValues.Properties valueProps, Properties properties) {
    super(varProps, valueProps);
    validateType(VariableType.LONGITUDE);
    _properties = properties;
  }

  public Long getPrecision() {
    return _properties.precision;
  }

  public static LongitudeVariable assertType(Variable variable) {
    if (variable instanceof LongitudeVariable) return (LongitudeVariable)variable;
    throw new BadRequestException("Variable " + variable.getId() +
        " of entity " + variable.getEntityId() + " is not a longitude variable.");
  }
}
