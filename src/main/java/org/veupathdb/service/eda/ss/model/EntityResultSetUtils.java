package org.veupathdb.service.eda.ss.model;

import java.sql.ResultSet;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.eda.ss.Resources;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.veupathdb.service.eda.ss.model.RdbmsColumnNames.*;

/**
 * utilities for creating Entities from result sets
 * @author Steve
 *
 */
public class EntityResultSetUtils {

  private final static String STDY_ABBRV_COL_NM = "study_abbrev"; // for private queries

  static TreeNode<Entity> getStudyEntityTree(DataSource datasource, String studyId) {
    
    String sql = generateEntityTreeSql(studyId);
    
    // entityID -> list of child entities
    Map<String, List<Entity>> simpleTree = new HashMap<>();
    
    Entity rootEntity = new SQLRunner(datasource, sql, "Get entity tree").executeQuery(rs -> {
      Entity root = null;
      while (rs.next()) {
        Entity entity = createEntityFromResultSet(rs);
        String parentId = rs.getString(ENTITY_PARENT_ID_COL_NAME);
        if (parentId == null) {
          if (root != null) throw new RuntimeException("In Study " + studyId + " found more than one root entity");
          root = entity;
        } else {
          if (!simpleTree.containsKey(parentId)) simpleTree.put(parentId, new ArrayList<>());
          simpleTree.get(parentId).add(entity);
        }
      }
      return root;
    });
    
    if (rootEntity == null) throw new RuntimeException("Found no entities for study: " + studyId);

    List<Entity> rootKids = simpleTree.get(rootEntity.getId());
    TreeNode<Entity> rootNode = new TreeNode<>(rootEntity);
    populateEntityTree(rootNode, rootKids, simpleTree);
    return rootNode;
  }
  
  static void populateEntityTree(TreeNode<Entity> parentNode, List<Entity> children, Map<String, List<Entity>> simpleTree) {
    for (Entity child : children) {
      TreeNode<Entity> childNode = new TreeNode<>(child);
      parentNode.addChildNode(childNode);
      
      // if this node has children, recurse
      if (simpleTree.containsKey(child.getId()))
        populateEntityTree(childNode, simpleTree.get(child.getId()), simpleTree);
    }
  }
  
  static String generateEntityTreeSql(String studyId) {
    String[] entityCols = {STUDY_ID_COL_NAME, ENTITY_ABBREV_COL_NAME, DISPLAY_NAME_COL_NAME, DISPLAY_NAME_PLURAL_COL_NAME, ENTITY_ID_COL_NAME, DESCRIP_COL_NAME, ENTITY_PARENT_ID_COL_NAME};
    return "SELECT e." + String.join(", e.", entityCols) + ", s." + STUDY_ABBREV_COL_NAME + " as " + STDY_ABBRV_COL_NM + NL
        + "FROM " + Resources.getAppDbSchema() + ENTITY_TABLE_NAME + " e," + Resources.getAppDbSchema() + STUDY_TABLE_NAME + " s " + NL
        + "WHERE s." + STUDY_ID_COL_NAME + " = '" + studyId + "'" + NL
        + "AND e." + ENTITY_STUDY_ID_COL_NAME + " = s." + STUDY_ID_COL_NAME  + NL
        + "ORDER BY e." + ENTITY_ID_COL_NAME;  // stable ordering supports unit testing
  }

  static Entity createEntityFromResultSet(ResultSet rs) {

    try {
      String name = getRsStringNotNull(rs, DISPLAY_NAME_COL_NAME);
      String namePlural = rs.getString(DISPLAY_NAME_PLURAL_COL_NAME);
      if (namePlural == null) namePlural = name + "s";  // TODO remove this hack when db has plurals
      String id = getRsStringNotNull(rs, ENTITY_ID_COL_NAME);
      String studyAbbrev = getRsStringNotNull(rs, STDY_ABBRV_COL_NM);
      String descrip = getRsStringWithDefault(rs, DESCRIP_COL_NAME, "No Entity Description available");
      String abbrev = getRsStringNotNull(rs, ENTITY_ABBREV_COL_NAME);

      return new Entity(id, studyAbbrev, name, namePlural, descrip, abbrev);
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * Tall table rows look like this:
   *   ancestor1_pk, ancestor2_pk, pk, variableA_id, string_value, number_value, date_value
   */
  static Map<String, String> resultSetToTallRowMap(Entity entity, ResultSet rs) {

    Map<String, String> tallRow = new HashMap<>();

    try {
      // add entity PK to map
      tallRow.put(entity.getPKColName(), rs.getString(entity.getPKColName()));
      // add ancestor PKs to map
      for (String colName : entity.getAncestorPkColNames()) {
        tallRow.put(colName, rs.getString(colName));
      }
      // add variable ID to map (may be null!)
      String variableId = rs.getString(TT_VARIABLE_ID_COL_NAME);
      tallRow.put(TT_VARIABLE_ID_COL_NAME, variableId);

      // add variable value to map (skip if ID is null)
      if (variableId != null) {
        Variable var = entity.getVariable(variableId)
          .orElseThrow(() -> new RuntimeException(
              "Metadata does not have variable found in tall table result set: " + variableId));

        tallRow.put(VARIABLE_VALUE_COL_NAME, var.getType().convertRowValueToStringValue(rs));
      }
      return tallRow;
    }
    catch (SQLException e) {
      throw new RuntimeException(e);
    }
  }
  /**
   * Return a function that transforms a list of tall table rows to a single wide row.
   * 
   * Tall table rows look like this:
   *   ancestor1_pk, ancestor2_pk, pk, variableA_id, value
   *   ancestor1_pk, ancestor2_pk, pk, variableB_id, value
   *   ancestor1_pk, ancestor2_pk, pk, variableC_id, value
   *   
   * Output wide row looks like this:
   *   ancestor1_pk, ancestor2_pk, pk, variableA_value, variableB_value, variableC_value
   *   
   *   (all values are converted to strings)
   */
  static Function<List<Map<String, String>>, Map<String, String>> getTallToWideFunction(Entity entity) {
    
    String errPrefix = "Tall row supplied to entity " + entity.getId();

    return tallRows -> {

      Map<String,String> firstTallRow = tallRows.get(0);
      Map<String, String> wideRow = new HashMap<>();

      // add entity PK to the wide row
      String tallRowEntityId = firstTallRow.get(entity.getPKColName());
      wideRow.put(entity.getPKColName(), tallRowEntityId);

      // add ancestor PKs to wide row
      for (String ancestorPkColName : entity.getAncestorPkColNames()) {
        if (!firstTallRow.containsKey(ancestorPkColName))
          throw new RuntimeException(errPrefix + " does not contain column " + ancestorPkColName);
        wideRow.put(ancestorPkColName, firstTallRow.get(ancestorPkColName));
      }

      // loop through all tall rows and add vars to wide row, validating along the way
      for (Map<String, String> tallRow : tallRows) {
        String variableId = tallRow.get(TT_VARIABLE_ID_COL_NAME);
        if (variableId != null) {
          validateTallRow(entity, tallRow, errPrefix, tallRowEntityId, variableId);
          wideRow.put(variableId, tallRow.get(VARIABLE_VALUE_COL_NAME));
        }
      }
      return wideRow;
    };
  }
  
  private static void validateTallRow(Entity entity, Map<String,String> tallRow, String errPrefix, String tallRowEnityId, String variableId) {
    // do some simple validation
    if (tallRow.size() != entity.getTallRowSize()) 
      throw new RuntimeException(errPrefix + " has an unexpected number of columns: (" + tallRow.size() + "): " + tallRow.keySet());
    
    if (!tallRow.get(entity.getPKColName()).equals(tallRowEnityId))
      throw new RuntimeException(errPrefix + " has an unexpected PK value");

    if (!tallRow.containsKey(TT_VARIABLE_ID_COL_NAME) )
      throw new RuntimeException(errPrefix + " does not contain column " + TT_VARIABLE_ID_COL_NAME);

    entity.getVariable(variableId)
        .orElseThrow(() -> new RuntimeException(errPrefix + " has an invalid variableId: " + variableId));
  }
  
}
