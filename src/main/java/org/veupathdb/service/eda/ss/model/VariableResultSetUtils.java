package org.veupathdb.service.eda.ss.model;

import org.gusdb.fgputil.db.runner.SQLRunner;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.veupathdb.service.eda.ss.Resources;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.veupathdb.service.eda.ss.model.RdbmsColumnNames.*;

class VariableResultSetUtils {

  static List<Variable> getEntityVariables(DataSource datasource, Entity entity) {
    
    String sql = generateStudyVariablesListSql(entity.getVariablesTableName());
    
    return new SQLRunner(datasource, sql, "Get entity variables metadata for: '" + entity.getDisplayName() + "'").executeQuery(rs -> {
      List<Variable> variables = new ArrayList<>();
      while (rs.next()) {
        variables.add(createVariableFromResultSet(rs, entity));
      }
      return variables;
    });
  }

  static String generateStudyVariablesListSql(String variablesTableName) {
    String[] selectCols = {VARIABLE_ID_COL_NAME, VARIABLE_TYPE_COL_NAME,
            DATA_SHAPE_COL_NAME, DISPLAY_TYPE_COL_NAME, HAS_VALUES_COL_NAME, UNITS_DISPLAY_NAME_COL_NAME, MULTIVALUED_COL_NAME, PRECISION_COL_NAME, PROVIDER_LABEL_COL_NAME, DISPLAY_NAME_COL_NAME, VARIABLE_PARENT_ID_COL_NAME};

//    return "SELECT " + String.join(", ", selectCols) + NL
    return "SELECT distinct " + String.join(", ", selectCols) + NL  // TODO: remove hack distinct
        + "FROM " + Resources.getAppDbSchema() + variablesTableName + NL
        + "ORDER BY " + VARIABLE_ID_COL_NAME;  // stable ordering supports unit testing
  }

  //   public Variable(String providerLabel, String id, Entity entity, VariableType type, VariableDataShape dataShape,
  //                  VariableDisplayType displayType, boolean hasValues, String units, Integer precision, String displayName, String parentId) {
  static Variable createVariableFromResultSet(ResultSet rs, Entity entity) {
    try {
      boolean hasValues = rs.getBoolean(HAS_VALUES_COL_NAME);
      String providerLabel = getRsStringWithDefault(rs, PROVIDER_LABEL_COL_NAME, "No Provider Label available");  // TODO remove hack when in db
      String id = getRsStringNotNull(rs, VARIABLE_ID_COL_NAME);
      String displayName = getRsStringNotNull(rs, DISPLAY_NAME_COL_NAME);
      String parentId = rs.getString(VARIABLE_PARENT_ID_COL_NAME);
      return hasValues ? createValueVarFromResultSet(rs, entity, providerLabel, id, displayName, parentId) :
              new Variable(providerLabel, id, entity, displayName, parentId);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  static Variable createValueVarFromResultSet(ResultSet rs, Entity entity, String providerLabel,
                                              String id, String displayName, String parentId) {

    try {
     return new Variable(
              providerLabel,
              id,
              entity,
              Variable.VariableType.fromString(getRsStringNotNull(rs, VARIABLE_TYPE_COL_NAME)),
              Variable.VariableDataShape.fromString(getRsStringNotNull(rs, DATA_SHAPE_COL_NAME)),
              Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
              getRsStringWithDefault(rs, UNITS_ID_COL_NAME, "NoUnitsIdAvailable"), // TODO remove hack default
              getRsStringWithDefault(rs, UNITS_DISPLAY_NAME_COL_NAME, "No Units Available"), // TODO remove hack default
              getRsStringWithDefault(rs, SCALE_ID_COL_NAME, "NoScaleIdAvailable"), // TODO remove hack default
              getRsStringWithDefault(rs, SCALE_DISPLAY_NAME_COL_NAME, "No Units Available"), // TODO remove hack default
              getRsIntegerWithDefault(rs, PRECISION_COL_NAME, 1), // TODO remove hack default
              displayName,
              parentId,
              false,  // TODO read isFeatured from the database
             false  // TODO read isTemporal from the database
      );
    }
    catch (SQLException e) {
      throw new RuntimeException("Entity:  " + entity.getId() + " variable: " + id, e);
    }
  }

}
