package org.veupathdb.service.eda.ss.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.sql.DataSource;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.json.JsonUtil;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.distribution.DistributionConfig;
import org.veupathdb.service.eda.ss.model.variable.DateVariable;
import org.veupathdb.service.eda.ss.model.variable.FloatingPointVariable;
import org.veupathdb.service.eda.ss.model.variable.IntegerVariable;
import org.veupathdb.service.eda.ss.model.variable.LongitudeVariable;
import org.veupathdb.service.eda.ss.model.variable.StringVariable;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableDataShape;
import org.veupathdb.service.eda.ss.model.variable.VariableDisplayType;
import org.veupathdb.service.eda.ss.model.variable.VariableType;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;
import org.veupathdb.service.eda.ss.model.variable.VariablesCategory;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.functional.Functions.doThrow;
import static org.veupathdb.service.eda.ss.model.RdbmsColumnNames.*;
import static org.veupathdb.service.eda.ss.model.ResultSetUtils.getDoubleFromString;
import static org.veupathdb.service.eda.ss.model.ResultSetUtils.getIntegerFromString;
import static org.veupathdb.service.eda.ss.model.ResultSetUtils.getRsIntegerWithDefault;
import static org.veupathdb.service.eda.ss.model.ResultSetUtils.getRsString;
import static org.veupathdb.service.eda.ss.model.ResultSetUtils.getRsStringWithDefault;

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
    String[] selectCols = {
        VARIABLE_ID_COL_NAME, VARIABLE_TYPE_COL_NAME,
        DATA_SHAPE_COL_NAME, DISPLAY_TYPE_COL_NAME, HAS_VALUES_COL_NAME, UNITS_COL_NAME, MULTIVALUED_COL_NAME,
        PRECISION_COL_NAME, PROVIDER_LABEL_COL_NAME, DISPLAY_NAME_COL_NAME, VARIABLE_PARENT_ID_COL_NAME,
        DEFINITION_COL_NAME, VOCABULARY_COL_NAME, DISPLAY_ORDER_COL_NAME, DISPLAY_RANGE_MIN_COL_NAME, DISPLAY_RANGE_MAX_COL_NAME,
        RANGE_MIN_COL_NAME, RANGE_MAX_COL_NAME, BIN_WIDTH_OVERRIDE_COL_NAME, BIN_WIDTH_COMPUTED_COL_NAME,
        IS_TEMPORAL_COL_NAME, IS_FEATURED_COL_NAME, IS_MERGE_KEY_COL_NAME, IS_REPEATED_COL_NAME,
        DISTINCT_VALUES_COUNT_COL_NAME, IS_MULTI_VALUED_COL_NAME, HIDE_FROM_COL_NAME
    };

    // This SQL safe from injection because entities declare their own table names (no parameters)
    // TODO: remove hack distinct
    return "SELECT distinct " + String.join(", ", selectCols) + NL
        + "FROM " + Resources.getAppDbSchema() + variablesTableName + NL
        + "ORDER BY " + VARIABLE_ID_COL_NAME;  // stable ordering supports unit testing
  }

  static Variable createVariableFromResultSet(ResultSet rs, Entity entity) throws SQLException {

    Variable.Properties varProps = new Variable.Properties(
        getRsStringWithDefault(rs, PROVIDER_LABEL_COL_NAME, "No Provider Label available"), // TODO remove hack when
                                                                                            // in db
        getRsString(rs, VARIABLE_ID_COL_NAME, true), entity,
        VariableDisplayType.fromString(getRsStringWithDefault(rs, DISPLAY_TYPE_COL_NAME, "default")),
        getRsString(rs, DISPLAY_NAME_COL_NAME, true), rs.getInt(DISPLAY_ORDER_COL_NAME),
        rs.getString(VARIABLE_PARENT_ID_COL_NAME), getRsStringWithDefault(rs, DEFINITION_COL_NAME, ""),
        jsonArrayStringToList(rs.getString(HIDE_FROM_COL_NAME)));

    return rs.getBoolean(HAS_VALUES_COL_NAME) ? createValueVarFromResultSet(rs, varProps)
        : new VariablesCategory(varProps);
  }
  
  // parse a string containing a json array into a List
  static List<String> jsonArrayStringToList(String jsonArrayString) {
    try {
      return jsonArrayString == null ? null
          : Arrays.asList(JsonUtil.Jackson.readValue(jsonArrayString, String[].class));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Can't parse string into json array: '" + jsonArrayString + "'");
    }
  }

  static Variable createValueVarFromResultSet(ResultSet rs, Variable.Properties varProps) {

    try {
      VariableWithValues.Properties valueProps = new VariableWithValues.Properties(
          VariableType.fromString(getRsString(rs, VARIABLE_TYPE_COL_NAME, true)),
          VariableDataShape.fromString(getRsString(rs, DATA_SHAPE_COL_NAME, true)),
          jsonArrayStringToList(rs.getString(VOCABULARY_COL_NAME)),
          rs.getInt(DISTINCT_VALUES_COUNT_COL_NAME),
          rs.getBoolean(IS_TEMPORAL_COL_NAME),
          rs.getBoolean(IS_FEATURED_COL_NAME),
          rs.getBoolean(IS_MERGE_KEY_COL_NAME),
          rs.getBoolean(IS_MULTI_VALUED_COL_NAME)
      );

      return switch(valueProps.type) {

        case NUMBER ->
            new FloatingPointVariable(varProps, valueProps,
                new DistributionConfig<>(
                    getDoubleFromString(rs, DISPLAY_RANGE_MIN_COL_NAME, false),
                    getDoubleFromString(rs, DISPLAY_RANGE_MAX_COL_NAME, false),
                    getDoubleFromString(rs, RANGE_MIN_COL_NAME, true),
                    getDoubleFromString(rs, RANGE_MAX_COL_NAME, true),
                    getDoubleFromString(rs, BIN_WIDTH_COMPUTED_COL_NAME, true),
                    getDoubleFromString(rs, BIN_WIDTH_OVERRIDE_COL_NAME, false)
                ),
                new FloatingPointVariable.Properties(
                    getRsStringWithDefault(rs, UNITS_COL_NAME, ""),
                    getRsIntegerWithDefault(rs, PRECISION_COL_NAME, 1L)
                )
            );

        case LONGITUDE ->
            new LongitudeVariable(varProps, valueProps, new LongitudeVariable.Properties(
                getRsIntegerWithDefault(rs, PRECISION_COL_NAME, 1L)
            ));

        case INTEGER ->
            new IntegerVariable(varProps, valueProps,
                new DistributionConfig<>(
                    getIntegerFromString(rs, DISPLAY_RANGE_MIN_COL_NAME, false),
                    getIntegerFromString(rs, DISPLAY_RANGE_MAX_COL_NAME, false),
                    getIntegerFromString(rs, RANGE_MIN_COL_NAME, true),
                    getIntegerFromString(rs, RANGE_MAX_COL_NAME, true),
                    getIntegerFromString(rs, BIN_WIDTH_COMPUTED_COL_NAME, true),
                    getIntegerFromString(rs, BIN_WIDTH_OVERRIDE_COL_NAME, false)
                ),
                new IntegerVariable.Properties(
                    getRsStringWithDefault(rs, UNITS_COL_NAME, "")
                )
            );

        case DATE ->
            new DateVariable(varProps, valueProps, new DateVariable.Properties(
                valueProps.dataShape,
                getRsString(rs, DISPLAY_RANGE_MIN_COL_NAME, false),
                getRsString(rs, DISPLAY_RANGE_MAX_COL_NAME, false),
                getRsString(rs, RANGE_MIN_COL_NAME, true),
                getRsString(rs, RANGE_MAX_COL_NAME, true),
                1,
                getRsString(rs, BIN_WIDTH_COMPUTED_COL_NAME, true),
                getRsString(rs, BIN_WIDTH_OVERRIDE_COL_NAME, false)
            ));

        case STRING ->
            new StringVariable(varProps, valueProps);

        default -> doThrow(() ->
            new RuntimeException("Entity:  " + varProps.entity.getId() +
              " variable: " + varProps.id + " has unrecognized type " + valueProps.type));
      };
    }
    catch (SQLException e) {
      throw new RuntimeException("Entity:  " + varProps.entity.getId() + " variable: " + varProps.id, e);
    }
  }
}
