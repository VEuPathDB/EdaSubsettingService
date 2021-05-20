package org.veupathdb.service.eda.ss.model;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.functional.FunctionalInterfaces.FunctionWithException;

import java.sql.ResultSet;

public class Variable {
  private final String providerLabel;
  private final String id;
  private final Entity entity;
  private final VariableType type;
  private final VariableDataShape dataShape;
  private final VariableDisplayType displayType;
  private final boolean hasValues;
  private final String unitsId;
  private final String scaleId;
  private final Integer precision;
  private final String displayName;
  private final String parentId;
  private final Boolean isFeatured;
  private final Boolean isTemporal;

  public boolean getHasValues() {
    return hasValues;
  }

  public VariableDisplayType getDisplayType() {
    return displayType;
  }

  public enum VariableType {
    STRING ("string_value", rs -> rs.getString("string_value"), "string"),  
    NUMBER ("number_value", rs -> String.valueOf(rs.getDouble("number_value")), "number"),
    DATE   ("date_value", rs -> FormatUtil.formatDateTimeNoTimezone(rs.getDate("date_value")), "date"),
    LONGITUDE ("number_value", rs -> String.valueOf(rs.getDouble("number_value")), "longitude");

    private final String tallTableColumnName;
    private final String typeString;
    private final FunctionWithException<ResultSet, String> resultSetToStringValue;

    VariableType(String tallTableColumnName, FunctionWithException<ResultSet, String> resultSetToStringValue, String typeString) {
      this.tallTableColumnName = tallTableColumnName;
      this.resultSetToStringValue = resultSetToStringValue;
      this.typeString = typeString;
    }
    
    public String getTallTableColumnName() {
      return this.tallTableColumnName;
    }

    public static VariableType fromString(String str) {
      if (str.equals(STRING.typeString) || str.equals("boolean")) return STRING;  // TODO remove boolean hack
      else if (str.equals(NUMBER.typeString)) return NUMBER;
      else if (str.equals(LONGITUDE.typeString)) return LONGITUDE;
      else if (str.equals(DATE.typeString)) return DATE;
      else throw new RuntimeException("Illegal variable type string: " + str);
    }
    
    public String convertRowValueToStringValue(ResultSet rs) {
      try {
        return resultSetToStringValue.apply(rs);
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  private final static String CONT_STR = "continuous";
  private final static String CAT_STR = "categorical";
  private final static String ORD_STR = "ordinal";
  private final static String TWO_STR = "binary";

  public enum VariableDataShape {
    CONTINUOUS(CONT_STR),
    CATEGORICAL(CAT_STR),
    ORDINAL(ORD_STR),
    BINARY(TWO_STR);

    private final String name;

    VariableDataShape(String name) {
      this.name = name;
    }

    public static VariableDataShape fromString(String shapeString) {

      VariableDataShape v;

      switch (shapeString) {
        case CONT_STR -> v = CONTINUOUS;
        case CAT_STR -> v = CATEGORICAL;
        case ORD_STR -> v = ORDINAL;
        case TWO_STR -> v = BINARY;
        default -> throw new RuntimeException("Unrecognized data shape: " + shapeString);
      }
      return v;
    }

    public String getName() { return name;}
  }

  public enum VariableDisplayType {
    DEFAULT("default"),
    HIDDEN("hidden"),
    MULTIFILTER("multifilter");

    String type;

    VariableDisplayType(String type) {
      this.type = type;
    }

    public static VariableDisplayType fromString(String displayType) {

      VariableDisplayType t;

      switch (displayType) {
        case "default" -> t = DEFAULT;
        case "multifilter" -> t = MULTIFILTER;
        case "hidden" -> t = HIDDEN;
        default -> throw new RuntimeException("Unrecognized variable display type: " + displayType);
      }
      return t;
    }

    public String getType() { return type; }
  }

  /*
  Construct a variable that does have values
   */
  public Variable(String providerLabel, String id, Entity entity, VariableType type, VariableDataShape dataShape,
                  VariableDisplayType displayType, String unitsId, String scaleId,
                  Integer precision, String displayName, String parentId, Boolean isFeatured, Boolean isTemporal) {

    String errPrefix = "In entity " + entity.getId() + " variable " + id + " has a null ";
    if (type == null) throw new RuntimeException(errPrefix + "data type");
    if (dataShape == null) throw new RuntimeException(errPrefix + "data shape");
    if (displayType == null) throw new RuntimeException(errPrefix + "display type");
    if (isFeatured == null) throw new RuntimeException(errPrefix + "is featured");
    if (isTemporal == null) throw new RuntimeException(errPrefix + "is temporal");
    if (type.equals(VariableType.NUMBER)) {
      if (precision == null) throw new RuntimeException(errPrefix + "precision");
    }

    this.providerLabel = providerLabel;
    this.id = id;
    this.entity = entity;
    this.type = type;
    this.dataShape = dataShape;
    this.displayType = displayType;
    this.hasValues = true;
    this.unitsId = unitsId;
    this.scaleId = scaleId;
    this.isFeatured = isFeatured;
    this.isTemporal = isTemporal;
    this.precision = precision;
    this.displayName = displayName;
    this.parentId = parentId;
  }

  /*
  Construct a variable that does not have values
   */
  public Variable(String providerLabel, String id, Entity entity, String displayName, String parentId) {
    this.providerLabel = providerLabel;
    this.id = id;
    this.entity = entity;
    this.type = null;
    this.dataShape = null;
    this.displayType = null;
    this.hasValues = false;
    this.unitsId = null;
    this.scaleId = null;
    this.precision = null;
    this.displayName = displayName;
    this.parentId = parentId;
    this.isFeatured = null;
    this.isTemporal = null;
  }

  public String getProviderLabel() {
    return providerLabel;
  }

  public String getId() {
    return id;
  }

  public String getEntityId() {
    return entity.getId();
  } 
  
  public Entity getEntity() {
    return entity;
  } 
  
  public VariableDataShape getDataShape() {
    return dataShape;
  }

  public String getUnitsId() {
    return unitsId;
  }

  public String getScaleId() {
    return scaleId;
  }

  public Integer getPrecision() {
    return precision;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getParentId() {
    return parentId;
  }

  public VariableType getType() {
    return type;
  }

  public Boolean getIsFeatured() {
    return isFeatured;
  }

  public Boolean getIsTemporal() {
    return isTemporal;
  }
}
