package org.veupathdb.service.eda.ss.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

  private static final Logger LOG = LogManager.getLogger(VariableResultSetUtils.class);

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
    String[] selectCols = {
        VARIABLE_ID_COL_NAME,
        VARIABLE_TYPE_COL_NAME,
        DATA_SHAPE_COL_NAME,
        DISPLAY_TYPE_COL_NAME,
        HAS_VALUES_COL_NAME,
        MULTIVALUED_COL_NAME,
        PRECISION_COL_NAME,
        UNITS_ID_COL_NAME,
        //SCALE_ID_COL_NAME, TODO: add in when data available
        //IS_FEATURED_COL_NAME, TODO: add in when data available
        //ID_TEMPORAL_COL_NAME, TODO: add in when data available
        PROVIDER_LABEL_COL_NAME,
        DISPLAY_NAME_COL_NAME,
        VARIABLE_PARENT_ID_COL_NAME};

//    return "SELECT " + String.join(", ", selectCols) + NL
    return "SELECT distinct " + String.join(", ", selectCols) + NL  // TODO: remove hack distinct
        + "FROM " + Resources.getAppDbSchema() + variablesTableName + NL
        + "ORDER BY " + VARIABLE_ID_COL_NAME;  // stable ordering supports unit testing
  }

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
      Variable.VariableType type = Variable.VariableType.fromString(getRsStringNotNull(rs, VARIABLE_TYPE_COL_NAME));
      String scaleId = calculateValidScaleId(id, type, rs);
      return new Variable(
              providerLabel,
              id,
              entity,
              type,
              Variable.VariableDataShape.fromString(getRsStringNotNull(rs, DATA_SHAPE_COL_NAME)),
              Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
              getRsStringWithDefault(rs, UNITS_ID_COL_NAME, null),
              scaleId,
              getRsIntegerWithDefault(rs, PRECISION_COL_NAME, 1), // TODO remove hack default
              displayName,
              parentId,
              false, // rs.getBoolean(IS_FEATURED_COL_NAME),  // TODO read isFeatured from the database
              false // rs.getBoolean(IS_TEMPORAL_COL_NAME),  //// TODO read isTemporal from the database
      );
    }
    catch (SQLException e) {
      throw new RuntimeException("Entity:  " + entity.getId() + " variable: " + id, e);
    }
  }

  private static String calculateValidScaleId(String varId, Variable.VariableType type, ResultSet rs) throws SQLException {
    String scaleIdFromDb = null; // rs.getString(SCALE_ID_COL_NAME); // TODO read scale from the database
    if (Variable.VariableType.NUMBER.equals(type)) {
      return scaleIdFromDb != null ? scaleIdFromDb : UnitsAndScale.LINEAR_SCALE_ID;
    }
    else {
      if (scaleIdFromDb != null) {
        LOG.warn("Variable with ID " + varId + " is non-numeric but has a value set for scale.");
      }
      return null;
    }
  }

}
