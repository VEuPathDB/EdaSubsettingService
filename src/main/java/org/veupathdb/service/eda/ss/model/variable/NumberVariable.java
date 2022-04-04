package org.veupathdb.service.eda.ss.model.variable;

import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.ss.model.distribution.DistributionConfig;

/**
 * This is a superclass supporting ONLY float and integer types, which can both
 * be used in NumberRange and NumberSet filters and support numeric range distributions.
 */
public abstract class NumberVariable<T extends Number> extends VariableWithValues {

  public abstract T validateBinWidth(Number binWidth);
  public abstract String getUnits();

  private final DistributionConfig<T> _distributionConfig;

  public NumberVariable(Variable.Properties varProperties, Properties properties, DistributionConfig<T> distributionConfig) {
    super(varProperties, properties);
    _distributionConfig = distributionConfig;
  }

  public DistributionConfig<T> getDistributionConfig() {
    return _distributionConfig;
  }

  public static NumberVariable<?> assertType(Variable variable) {
    if (variable instanceof NumberVariable<?>) return (NumberVariable<?>)variable;
    throw new BadRequestException("Variable " + variable.getId() +
        " of entity " + variable.getEntityId() + " is not a number or integer variable.");
  }

  @Override
  public String getDownloadColHeader() {
    String units = getUnits();
    String unitsStr = units == null || units.isBlank() ? "" : " (" + units.trim() + ")";
    return getDisplayName() + unitsStr + " [" + getId() + "]";
  }
}
