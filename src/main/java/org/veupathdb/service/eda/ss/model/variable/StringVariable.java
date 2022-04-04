package org.veupathdb.service.eda.ss.model.variable;

import jakarta.ws.rs.BadRequestException;

public class StringVariable extends VariableWithValues {

  public StringVariable(Variable.Properties varProperties, VariableWithValues.Properties valueProperties) {
    super(varProperties, valueProperties);
    validateType(VariableType.STRING);
  }

  public static StringVariable assertType(Variable variable) {
    if (variable instanceof StringVariable) return (StringVariable)variable;
    throw new BadRequestException("Variable " + variable.getId() +
        " of entity " + variable.getEntityId() + " is not a string variable.");
  }
}
