package org.veupathdb.service.eda.ss.model;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.eda.ss.model.Variable.VariableDataShape;
import org.veupathdb.service.eda.ss.model.Variable.VariableType;
import org.veupathdb.service.eda.ss.model.filter.DateRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.DateSetFilter;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.model.filter.NumberRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.NumberSetFilter;
import org.veupathdb.service.eda.ss.model.filter.StringSetFilter;

/**
 * A class that holds a test model of a study and supporting objects
 * @author Steve
 *
 */
public class TestModel {

  // reusable study objects
  public Study study; 
  
  public Entity household;
  public Entity householdObs;
  public Entity participant;
  public Entity observation;
  public Entity sample;
  public Entity treatment;
  
  public Variable roof;
  public Variable shoesize;
  public Variable weight;
  public Variable favNumber;
  public Variable birthDate;
  public Variable favNewYears;
  public Variable mood;
  public Variable haircolor;
  public Variable networth;
  public Variable waterSupply;
  
  public Filter obsWeightFilter;
  public Filter houseRoofFilter;
  public Filter obsFavNumberFilter; // categorical numeric
  public Filter obsBirthDateFilter;  // continuous date
  public Filter obsFavNewYearsFilter;  // categorical numeric
  public Filter obsMoodFilter; // string 
  public Filter houseObsWaterSupplyFilter; // string 
  
  public TestModel() {
    createTestEntities();
    Study.StudyOverview overview = new Study.StudyOverview("GEMS", "datasetid_2222", "555555");
    study = new Study(overview, constructEntityTree(), createIdMap());
    constructVariables();
    createFilters();
  }
  
  private void createTestEntities() {
    household = new Entity("GEMS_House", "555555", "Household", "Households", "descrip", "Hshld");
    householdObs = new Entity("GEMS_HouseObs", "555555", "Household Observation", "Household Observations", "descrip", "HshldObsvtn");
    participant = new Entity("GEMS_Part", "555555", "Participant", "Participants", "descrip", "Prtcpnt");
    observation = new Entity("GEMS_PartObs", "555555", "Observation", "Observations", "descrip", "PrtcpntObsrvtn");
    sample = new Entity("GEMS_Sample", "555555", "Sample", "Samples", "descrip", "Smpl");
    treatment = new Entity("GEMS_Treat", "555555", "Treatment", "Treatments", "descrip", "Trtmnt");
  }
  
  private Map<String, Entity> createIdMap() {
    Map<String, Entity> idMap = new HashMap<>();
    idMap.put("GEMS_House", household);
    idMap.put("GEMS_HouseObs", householdObs);
    idMap.put("GEMS_Part", participant);
    idMap.put("GEMS_PartObs", observation);
    idMap.put("GEMS_Sample", sample);
    idMap.put("GEMS_Treat", treatment);
    return idMap;
   }

  /*
   * return a fresh entity tree.
   */
  public TreeNode<Entity> constructEntityTree() {
    TreeNode<Entity> householdNode = new TreeNode<>(household);

    TreeNode<Entity> houseObsNode = new TreeNode<>(householdObs);
    householdNode.addChildNode(houseObsNode);

    TreeNode<Entity> participantNode = new TreeNode<>(participant);
    householdNode.addChildNode(participantNode);

    TreeNode<Entity> observationNode = new TreeNode<>(observation);
    participantNode.addChildNode(observationNode);
    
    TreeNode<Entity> sampleNode = new TreeNode<>(sample);
    observationNode.addChildNode(sampleNode);
    
    TreeNode<Entity> treatmentNode = new TreeNode<>(treatment);
    observationNode.addChildNode(treatmentNode);
    
    return householdNode;
  }
  
  private void constructVariables() {

    //  public Variable(String providerLabel, String id, Entity entity, VariableType type, VariableDataShape dataShape,
    //                  VariableDisplayType displayType, boolean hasValues, String units, Integer precision, String displayName, String parentId) {
    roof = new Variable("roof", "var_10", household, VariableType.STRING, VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    household.addVariable(roof);
    
    shoesize = new Variable("shoesize", "var_11", participant, VariableType.NUMBER, VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    participant.addVariable(shoesize);

    haircolor = new Variable("haircolor", "var_17", participant, VariableType.STRING, Variable.VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    participant.addVariable(haircolor);

    networth = new Variable("networth", "var_18", participant, VariableType.NUMBER, VariableDataShape.CONTINUOUS,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    participant.addVariable(networth);

    weight = new Variable("weight", "var_12", observation, VariableType.NUMBER, Variable.VariableDataShape.CONTINUOUS,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    observation.addVariable(weight);
    
    favNumber = new Variable("favNumber", "var_13", observation, VariableType.NUMBER, Variable.VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    observation.addVariable(favNumber);
    
    birthDate  = new Variable("birthDate", "var_14", observation, VariableType.DATE, Variable.VariableDataShape.CONTINUOUS,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    observation.addVariable(birthDate);
    
    favNewYears = new Variable("favNewYears", "var_15", observation, VariableType.DATE, Variable.VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    observation.addVariable(favNewYears);
    
    mood  = new Variable("mood", "var_16", observation, VariableType.STRING, VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT,"", "", "", "", 1, "Roof", null, false, false);
    observation.addVariable(mood);

    waterSupply  = new Variable("waterSupply", "var_19", householdObs, VariableType.STRING, VariableDataShape.CATEGORICAL,
            Variable.VariableDisplayType.DEFAULT, "", "", "", "", 1, "Roof", null, false, false);
    householdObs.addVariable(waterSupply);

  }
  
  private void createFilters() {
    
    // create observation weight filter
    obsWeightFilter = new NumberRangeFilter(observation, weight.getId(), 10, 20);

    List<Number> favNums = Arrays.asList(new Number[]{5,7,9});
    obsFavNumberFilter = new NumberSetFilter(observation, favNumber.getId(), favNums);

    obsBirthDateFilter = new DateRangeFilter(observation, birthDate.getId(),
        LocalDateTime.of(2019, Month.MARCH, 21, 0, 0),
        LocalDateTime.of(2019, Month.MARCH, 28, 0, 0));

    List<LocalDateTime> dates = new ArrayList<>();
    dates.add(LocalDateTime.of(2019, Month.MARCH, 21, 0, 0));
    dates.add(LocalDateTime.of(2019, Month.MARCH, 28, 0, 0));
    dates.add(LocalDateTime.of(2019, Month.JUNE, 12, 0, 0));
    obsFavNewYearsFilter = new DateSetFilter(observation, favNewYears.getId(), dates);

    List<String> moods = Arrays.asList("happy", "jolly", "giddy");
    obsMoodFilter = new StringSetFilter(observation, mood.getId(), moods);

    obsWeightFilter = new NumberRangeFilter(observation, weight.getId(), 10, 20);

    // create household roof filter
    List<String> roofs = Arrays.asList("metal", "tile");
    houseRoofFilter = new StringSetFilter(household, roof.getId(), roofs);

    // create household observation filter
    List<String> waterSupplies = Arrays.asList("piped", "well");
    houseObsWaterSupplyFilter = new StringSetFilter(householdObs, waterSupply.getId(), waterSupplies);
  }
}
