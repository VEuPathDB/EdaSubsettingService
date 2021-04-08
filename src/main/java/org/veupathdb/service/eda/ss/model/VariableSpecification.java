package org.veupathdb.service.eda.ss.model;

public class VariableSpecification {
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

    public String getUnitsId() {
        return unitsId;
    }

    public String getScaleId() {
        return scaleId;
    }
}
