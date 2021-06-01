
package org.veupathdb.service.eda.ss.model;

import java.util.Collection;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.gusdb.fgputil.Tuples;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.veupathdb.service.eda.common.model.VariableDef;

/**
 * @author Steve
 *
 */
public class Entity {
  private final String id;
  private final String studyAbbrev; // internal abbrev
  private final String displayName;
  private final String displayNamePlural;
  private final String description;
  private final String abbreviation;

  private final Map<String, Variable> variablesMap = new HashMap<>();
  private final List<Variable> variablesList = new ArrayList<>();
  private List<Entity> ancestorEntities;
  private List<String> ancestorPkColNames;
  private List<String> ancestorFullPkColNames; // entityName.pkColName
  private List<TwoTuple<String,String>> ancestorPkColVariables;
  private Integer tallRowSize; // number of columns in a tall table row
  
  public Entity(String entityId, String studyAbbrev, String displayName, String displayNamePlural, String description, String abbreviation) {
    this.id = entityId;
    this.studyAbbrev = studyAbbrev;
    this.displayName = displayName;
    this.displayNamePlural = displayNamePlural;
    this.description = description;
    this.abbreviation = abbreviation;
  }

  public String getAbbreviation() {
    return abbreviation;
  }

  public String getId() {
    return id;
  }

  public String getStudyAbbrev() {
    return studyAbbrev;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDisplayNamePlural() {
    return displayNamePlural;
  }

  public String getDescription() {
    return description;
  }

  public String getVariablesTableName() { return "AttributeGraph_" + getStudyAbbrev() + "_" + getAbbreviation(); }

  public String getTallTableName() {
    return "AttributeValue_" + getStudyAbbrev() + "_" + getAbbreviation();
  }

  public String getPKColName() {
    return getAbbreviation() + "_stable_id";
  }
  
  public String getFullPKColName() {
    return getWithClauseName() + "." + getPKColName();
  }
  
  public String getAncestorsTableName() {
    return "Ancestors_" + getStudyAbbrev() + "_" + getAbbreviation();
  }

  public String getWithClauseName() {
    return id;
  }
  
  public List<String> getAncestorPkColNames() {
    return Collections.unmodifiableList(ancestorPkColNames);
  }
  
  public Optional<Variable> getVariable(String variableId) {
    return Optional.ofNullable(variablesMap.get(variableId));
  }
  
  public List<String> getAncestorFullPkColNames() {
    return Collections.unmodifiableList(ancestorFullPkColNames);
  }
  
  public void setAncestorEntities(List<Entity> ancestorEntities) {
    this.ancestorEntities = new ArrayList<>(ancestorEntities);
    this.ancestorPkColNames = 
        ancestorEntities.stream().map(Entity::getPKColName).collect(Collectors.toList());
    this.ancestorFullPkColNames = 
        ancestorEntities.stream().map(Entity::getFullPKColName).collect(Collectors.toList());
    this.ancestorPkColVariables =
        ancestorEntities.stream().map(e -> new TwoTuple<>(e.getId(), e.getPKColName())).collect(Collectors.toList());
  }

  public List<Entity> getAncestorEntities() {
    return Collections.unmodifiableList(ancestorEntities);
  }
  
  public String toString() {
    return "id: " + getId() + " name: " + getDisplayName() + " (" + super.toString() + ")";
  }
  
  public String getAllPksSelectList(String entityTableName, String ancestorTableName) {
    List<String> selectColsList = new ArrayList<>();
    selectColsList.add(entityTableName + "." + getPKColName());
    for (String name : getAncestorPkColNames())
      selectColsList.add(ancestorTableName + "." + name);
    return String.join(", ", selectColsList);
  }
  
  // ancestor PKs, pk, variable_id, value
  Integer getTallRowSize() {
    if (tallRowSize == null) tallRowSize = ancestorEntities.size() + 3;
    return tallRowSize;
  }
  
  public List<Variable> getVariables() {
    return Collections.unmodifiableList(variablesList);
  }
 
  void addVariable(Variable var) {
    if (variablesMap.containsKey(var.getId()))
      throw new RuntimeException("In Entity '" + getId() + "', trying to add duplicate variable: " + var.getId());
    variablesMap.put(var.getId(), var);
    variablesList.add(var);
  }

  void loadVariables(DataSource datasource) {
    List<Variable> variables = VariableResultSetUtils.getEntityVariables(datasource, this);
    for (Variable var : variables) addVariable(var);
  }

  public List<TwoTuple<String, String>> getAncestorPkVariables() {
    return ancestorPkColVariables;
  }
}
