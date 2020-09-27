package org.veupathdb.service.edass.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import javax.ws.rs.InternalServerErrorException;

import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.edass.generated.model.APIFilter;


public class Study {
  private String studyId;
  private TreeNode<Entity> entityTree;
  private Map<String, Entity> entityIdMap = new HashMap<String, Entity>();
  private Map<String, Variable> variablesMap;  // name -> Variable
  private Map<String, Entity> variableIdToEntityMap;
  
  public Study(String studyId) {
    this.studyId = studyId;
  }
  
  public void initializeStudy(DataSource datasource) {
    validateStudyId(datasource, studyId);
    TreeNode<Entity> entityTree = loadEntityTree(datasource);
    Set<Variable> variables = loadVariables(datasource);
    initEntitiesAndVariables(entityTree, variables);
  }
  
  /** 
   * 
   * @param entityId
   * @param variableIds
   * @return list of error messages, if any
   */
  // TODO
  public List<String> validateEntityVariables(String entityId, List<String> variableIds) {
    return null;
  }
  
  /**
   * 
   * @param apiFilters
   * @return list of error messages, if any
   */
  // TODO
  public List<String> validateApiFilters(List<APIFilter> apiFilters) {
    return null;
  }
  
  private Set<Variable> loadVariables(DataSource datasource) {
    return null;
  }

  private void validateStudyId(DataSource datasource, String studyId) {
    
  }
  
  /*
   * get the full entity tree for this study from the datasource.
   */
  private TreeNode<Entity> loadEntityTree(DataSource datasource) {
    // TODO
    return null;
  }
  
  /**
   * Build internal (convenience) state from the raw entity tree and variables set
   * @param rootEntityNode
   * @param vars
   */
  void initEntitiesAndVariables(TreeNode<Entity> rootEntityNode, Set<Variable> vars) {
    entityTree = rootEntityNode;
    validateEntityTreeIds(rootEntityNode);
    
    // build entity ID map
    entityIdMap = new HashMap<String, Entity>();
    rootEntityNode.flatten().stream().map(e -> entityIdMap.put(e.getEntityId(), e));

    // give each entity a set of its ancestor entities.
    populateEntityAncestors(rootEntityNode);
    
    variablesMap = new HashMap<String, Variable>();
    vars.stream().map(v -> variablesMap.put(v.getName(), v));

    variableIdToEntityMap = new HashMap<String, Entity>();
    vars.stream().map(v -> variableIdToEntityMap.put(v.getName(), entityIdMap.get(v.getEntityId())));
   }

  public Entity getEntity(String entityId) {
    return entityIdMap.get(entityId);
  }
   
  public TreeNode<Entity> getEntityTree() {
    return entityTree.clone();
  }
  
  private static void populateEntityAncestors(TreeNode<Entity> rootEntityNode) {
    populateEntityAncestors(rootEntityNode, new ArrayList<>());
  }
  
  private static void populateEntityAncestors(TreeNode<Entity> entityNode, List<Entity> ancestorEntities) {
    Entity entity = entityNode.getContents();
    entity.setAncestorEntities(ancestorEntities);
    ancestorEntities.add(entity);
    for (TreeNode<Entity> childNode : entityNode.getChildNodes()) {
      populateEntityAncestors(childNode, ancestorEntities);
    }
  }
  
  /*
   * Confirm that children have non-conflicting entity IDs. Throw runtime exception if invalid
   */
  private static void validateEntityTreeIds(TreeNode<Entity> entityNode) {
    String errPrefix = "In entity " + entityNode.getContents().getEntityId() +
        ", found a child with the same ID as ";

    Set<String> childEntityIds = new HashSet<String>();
    for (TreeNode<Entity> child : entityNode.getChildNodes()) {
      Entity childEntity = child.getContents();
      if (childEntity.equals(entityNode.getContents().getEntityId()))
        throw new InternalServerErrorException(errPrefix + "the parent: " + childEntity.getEntityId());

      if (childEntityIds.contains(childEntity.getEntityId()))
        throw new InternalServerErrorException(errPrefix + "another child: " + childEntity.getEntityId());
    }
  }

}
