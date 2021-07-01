package org.veupathdb.service.eda.ss.model;

import org.veupathdb.service.eda.generated.model.VariableSpec;

public class VariableSpecification implements VariableSpec {

  private final Variable variable;
  private final String unitsId;
  private final String scaleId;

  public VariableSpecification(Variable variable, String unitsId, String scaleId) {
      this.variable = variable;
      this.unitsId = unitsId;
      this.scaleId = scaleId;
  }

  public Variable getVariable() {
      return variable;
  }

  @Override
  public String getEntityId() {
    return variable.getEntityId();
  }

  @Override
  public String getVariableId() {
    return variable.getId();
  }

  @Override
  public String getUnitsId() {
      return unitsId;
  }

  @Override
  public String getScaleId() {
      return scaleId;
  }

  @Override
  public void setEntityId(String entityId) {
    throw new UnsupportedOperationException("Immutable implementation");
  }

  @Override
  public void setVariableId(String variableId) {
    throw new UnsupportedOperationException("Immutable implementation");
  }

  @Override
  public void setUnitsId(String unitsId) {
    throw new UnsupportedOperationException("Immutable implementation");
  }

  @Override
  public void setScaleId(String scaleId) {
    throw new UnsupportedOperationException("Immutable implementation");
  }
}
