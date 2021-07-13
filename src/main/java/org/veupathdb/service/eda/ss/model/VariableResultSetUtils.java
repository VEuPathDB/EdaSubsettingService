package org.veupathdb.service.eda.ss.model;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.json.JsonUtil;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Variable.VariableDisplayType;
import org.veupathdb.service.eda.ss.model.Variable.VariableType;

import com.fasterxml.jackson.core.JsonProcessingException;
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
            DATA_SHAPE_COL_NAME, DISPLAY_TYPE_COL_NAME, HAS_VALUES_COL_NAME, UNITS_COL_NAME, MULTIVALUED_COL_NAME, 
            PRECISION_COL_NAME, PROVIDER_LABEL_COL_NAME, DISPLAY_NAME_COL_NAME, VARIABLE_PARENT_ID_COL_NAME,
            DEFINITION_COL_NAME, VOCABULARY_COL_NAME, DISPLAY_ORDER_COL_NAME, DISPLAY_RANGE_MIN_COL_NAME, DISPLAY_RANGE_MAX_COL_NAME,
            RANGE_MIN_COL_NAME, RANGE_MAX_COL_NAME, BIN_WIDTH_OVERRIDE_COL_NAME, BIN_WIDTH_COMPUTED_COL_NAME, 
            IS_TEMPORAL_COL_NAME, IS_FEATURED_COL_NAME, IS_MERGE_KEY_COL_NAME, IS_REPEATED_COL_NAME,
            DISTINCT_VALUES_COUNT_COL_NAME, IS_MULTI_VALUED_COL_NAME
            };


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
      VariableDisplayType displayType = Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default"));

      return hasValues ? 
        createValueVarFromResultSet(rs, entity, providerLabel, id, displayName, parentId) :
        new Variable(providerLabel, id, entity, displayType, displayName, rs.getInt(DISPLAY_ORDER_COL_NAME), parentId);
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }

  static Variable createValueVarFromResultSet(ResultSet rs, Entity entity, String providerLabel,
                                              String id, String displayName, String parentId) {

    try {
    	// first, parse vocabulary json string
    	String vocabString = rs.getString(VOCABULARY_COL_NAME);
    	List<String> vocab = rs.wasNull()? null : Arrays.asList(JsonUtil.Jackson.readValue(vocabString, String[].class));

        VariableType type = Variable.VariableType.fromString(getRsStringNotNull(rs, VARIABLE_TYPE_COL_NAME));

    	if (type == VariableType.NUMBER || type == VariableType.LONGITUDE) return new NumberVariable(
                providerLabel,
                id,
                entity,
                type == VariableType.LONGITUDE,
                Variable.VariableDataShape.fromString(getRsStringNotNull(rs, DATA_SHAPE_COL_NAME)),
                Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
                rs.getInt(DISPLAY_ORDER_COL_NAME),
                getRsStringWithDefault(rs, UNITS_COL_NAME, ""), 
                getRsIntegerWithDefault(rs, PRECISION_COL_NAME, 1),
                displayName,
                parentId,
                getRsStringWithDefault(rs, DEFINITION_COL_NAME, ""),
                vocab,
                getNumberFromStringColumn(rs.getString(DISPLAY_RANGE_MIN_COL_NAME)), 
                getNumberFromStringColumn(rs.getString(DISPLAY_RANGE_MAX_COL_NAME)),
                getNumberFromStringColumn(rs.getString(RANGE_MIN_COL_NAME)),
                getNumberFromStringColumn(rs.getString(RANGE_MAX_COL_NAME)),
                getNumberFromStringColumn(rs.getString(BIN_WIDTH_OVERRIDE_COL_NAME)),
                getNumberFromStringColumn(rs.getString(BIN_WIDTH_COMPUTED_COL_NAME)),
                rs.getBoolean(IS_TEMPORAL_COL_NAME),
                rs.getBoolean(IS_FEATURED_COL_NAME),
                rs.getBoolean(IS_MERGE_KEY_COL_NAME),
                rs.getInt(DISTINCT_VALUES_COUNT_COL_NAME),
                rs.getBoolean(IS_MULTI_VALUED_COL_NAME)
        );
    	
    	
    	if (type == VariableType.DATE) return new DateVariable(
                providerLabel,
                id,
                entity,
                Variable.VariableDataShape.fromString(getRsStringNotNull(rs, DATA_SHAPE_COL_NAME)),
                Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
                displayName,
                rs.getInt(DISPLAY_ORDER_COL_NAME),
                parentId,
                getRsStringWithDefault(rs, DEFINITION_COL_NAME, ""),
                vocab,
                rs.getString(DISPLAY_RANGE_MIN_COL_NAME), 
                rs.getString(DISPLAY_RANGE_MAX_COL_NAME),
                rs.getString(RANGE_MIN_COL_NAME),
                rs.getString(RANGE_MAX_COL_NAME),
                rs.getString(BIN_WIDTH_OVERRIDE_COL_NAME),
                rs.getString(BIN_WIDTH_COMPUTED_COL_NAME),
                rs.getBoolean(IS_TEMPORAL_COL_NAME),
                rs.getBoolean(IS_FEATURED_COL_NAME),
                rs.getBoolean(IS_MERGE_KEY_COL_NAME),
                rs.getInt(DISTINCT_VALUES_COUNT_COL_NAME),
                rs.getBoolean(IS_MULTI_VALUED_COL_NAME)
        );
    	
    	return new StringVariable(
                providerLabel,
                id,
                entity,
                Variable.VariableDataShape.fromString(getRsStringNotNull(rs, DATA_SHAPE_COL_NAME)),
                Variable.VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
                displayName,
                rs.getInt(DISPLAY_ORDER_COL_NAME),
                parentId,
                getRsStringWithDefault(rs, DEFINITION_COL_NAME, ""),
                vocab,
                rs.getBoolean(IS_TEMPORAL_COL_NAME),
                rs.getBoolean(IS_FEATURED_COL_NAME),
                rs.getBoolean(IS_MERGE_KEY_COL_NAME),
                rs.getInt(DISTINCT_VALUES_COUNT_COL_NAME),
                rs.getBoolean(IS_MULTI_VALUED_COL_NAME)
        );  
    	
    }
    catch (SQLException | JsonProcessingException e) {
      throw new RuntimeException("Entity:  " + entity.getId() + " variable: " + id, e);
    }
  }

  private static Number getNumberFromStringColumn(String colValue) {
	  if (colValue == null) return null;
	  return Float.parseFloat(colValue);
  }
}
