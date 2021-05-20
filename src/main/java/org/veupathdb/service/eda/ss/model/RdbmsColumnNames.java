package org.veupathdb.service.eda.ss.model;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RdbmsColumnNames {

  static final String VARIABLE_VALUE_COL_NAME = "value";
  
  // common
  static final String DESCRIP_COL_NAME = "description";
  static final String DISPLAY_NAME_COL_NAME = "display_name";
  static final String DISPLAY_NAME_PLURAL_COL_NAME = "display_name_plural";

  // Study table
  static final String STUDY_TABLE_NAME = "study";
  static final String STUDY_ID_COL_NAME = "stable_id";
  static final String STUDY_ABBREV_COL_NAME = "internal_abbrev";

  // StudyIdDatasetId table
  static final String STUDY_DATASET_TABLE_NAME = "StudyIdDatasetId";
  static final String STUDY_DATASET_ID_COL_NAME = "dataset_id";
  static final String STUDY_DATASET_STUDY_ID_COL_NAME = "study_stable_id";

  // Entity table and EntityName table
  static final String ENTITY_TABLE_NAME = "EntityTypeGraph";
  static final String ENTITY_ID_COL_NAME = "stable_id";
  static final String ENTITY_STUDY_ID_COL_NAME = "study_stable_id";
  static final String ENTITY_PARENT_ID_COL_NAME = "parent_stable_id";
  static final String ENTITY_ABBREV_COL_NAME = "internal_abbrev";

  // Variable table and VariableType table
  static final String VARIABLE_ID_COL_NAME = "stable_id";
  static final String VARIABLE_TYPE_COL_NAME = "data_type";
  static final String PROVIDER_LABEL_COL_NAME = "provider_label";
  static final String DATA_SHAPE_COL_NAME = "data_shape";
  static final String DISPLAY_TYPE_COL_NAME = "term_type";
  static final String HAS_VALUES_COL_NAME = "has_values";
  static final String MULTIVALUED_COL_NAME = "is_multi_valued";
  static final String UNITS_ID_COL_NAME = "unit";
  static final String SCALE_ID_COL_NAME = "scale"; // TODO confirm column name
  static final String PRECISION_COL_NAME = "precision";
  static final String IS_FEATURED_COL_NAME = "is_featured"; // TODO confirm column name
  static final String ID_TEMPORAL_COL_NAME = "is_temporal"; // TODO confirm column name
  static final String VARIABLE_PARENT_ID_COL_NAME = "parent_stable_id";
  
  // Tall table
  public static final String TT_VARIABLE_ID_COL_NAME = "attribute_stable_id";
  public static final String STRING_VALUE_COL_NAME = "string_value";
  public static final String DATE_VALUE_COL_NAME = "date_value";
  public static final String NUMBER_VALUE_COL_NAME = "number_value";
  
  static String getRsStringNotNull(ResultSet rs, String colName) throws SQLException {
    if (rs.getString(colName) == null) 
      throw new RuntimeException("Found a null for variable column: " + colName);
    return rs.getString(colName);
  }

  static String getRsStringWithDefault(ResultSet rs, String colName, String defaultVal) throws SQLException {
    String val = rs.getString(colName);
    if (val == null) val = defaultVal;
    return val;
  }

  static Integer getRsIntegerWithDefault(ResultSet rs, String colName, Integer defaultVal) throws SQLException {
    Integer val = rs.getInt(colName);
    if (rs.wasNull()) val = defaultVal;
    return val;
  }

}
