package org.veupathdb.service.eda.ss.model;

import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.DelimitedDataParser;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.common.model.VariableDef.PkVariableDef;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.stubdb.StubDb;

import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.Stream;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.gusdb.fgputil.FormatUtil.TAB;
import static org.junit.jupiter.api.Assertions.*;
import static org.veupathdb.service.eda.common.model.VariableDef.toDotNotation;
import static org.veupathdb.service.eda.ss.model.RdbmsColumnNames.*;

public class StudySubsettingUtilsTest {

  private static final Logger LOG = LogManager.getLogger(StudySubsettingUtilsTest.class);

  private static TestModel _model;
  private static DataSource _dataSource;
  private static FiltersForTesting _filtersForTesting;

  @BeforeAll
  public static void setUp() {
    _model = new TestModel();
    _dataSource = StubDb.getDataSource();
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);
    _filtersForTesting = new FiltersForTesting(study);
  }

  @Test
  @DisplayName("Test getting set of entity IDs from set of filters ")
  void testGetEntityIdsInFilters() {
   
    // add it to a set
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    filters.add(_model.houseRoofFilter);
    
    List<String> entityIdsInFilters = StudySubsettingUtils.getEntityIdsInFilters(filters);

    assertEquals(2, entityIdsInFilters.size(), "ID set has incorrect size");
    assertTrue(entityIdsInFilters.contains(_model.observation.getId()), "ID set does not contain observ.");
    assertTrue(entityIdsInFilters.contains(_model.household.getId()), "ID set does not contain household.");
  }
  
  @Test
  @DisplayName("Test pruning an entity tree using 1 filter below")
  void testPruning1() {
    
    // create filter set with obs filter
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    
    // set output entity
    Entity outputEntity = _model.household;
    
    // prune tree
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, outputEntity);
    
     // construct expected tree
    TreeNode<Entity> expectedTree = new TreeNode<>(_model.household);
    expectedTree.addChild(_model.observation);

    // compare
    assertTrue(compareEntityTrees(prunedTree, expectedTree));
  }
  
  @Test
  @DisplayName("Test pruning an entity tree using 1 filter above")
  void testPruning2() {
    
    // add household roof filter to set
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.houseRoofFilter);
    
    // set output entity
    Entity outputEntity = _model.observation;
    
    // prune tree
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, outputEntity);
    
     // construct expected tree
    TreeNode<Entity> expectedTree = new TreeNode<>(_model.household);
    expectedTree.addChild(_model.observation);

    // compare
    assertTrue(compareEntityTrees(prunedTree, expectedTree));
  }
  
  @Test
  @DisplayName("Test pruning an entity tree with a pivot")
  void testPruning3() {
    
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    // set output entity
    Entity outputEntity = _model.householdObs;
    
    // prune tree
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, outputEntity);
    
     // construct expected tree
    TreeNode<Entity> expectedTree = new TreeNode<>(_model.household);
    expectedTree.addChild(_model.householdObs);
    expectedTree.addChild(_model.observation);

    /*
    System.out.println("Expected Tree: " + expectedTree);
    System.out.println("Pruned Tree: " + prunedTree);
    */
    
    // compare
    assertTrue(compareEntityTrees(prunedTree, expectedTree));
  }
  
  static boolean compareEntityTrees(TreeNode<Entity> t1, TreeNode<Entity> t2) {
    if ((t1 == null && t2 != null) || (t1 != null && t2 == null)) return false;
    
    if (t1 == null) return true; // both are null
    
    if (!t1.getContents().getId().equals(t2.getContents().getId())) return false;
    
    List<TreeNode<Entity>> t1Kids = t1.getChildNodes();
    List<TreeNode<Entity>> t2Kids = t2.getChildNodes();
    
    if (t1Kids.size() != t2Kids.size()) return false;
    
    for (int i=0; i < t1Kids.size(); i++)
      if (!compareEntityTrees(t1Kids.get(i), t2Kids.get(i))) return false;
    
    return true;
  }

  @Test
  @DisplayName("Test creating a WITH clause without any relevant filters")
  void testWithClauseNoFilters() {
    
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    filters.add(_model.obsFavNewYearsFilter);
    String withClause = StudySubsettingUtils.generateWithClause(_model.householdObs, filters);
    String expectedWithClause = _model.householdObs.getWithClauseName() + " as (" + NL +
        "  SELECT " + _model.household.getPKColName() + ", " + _model.householdObs.getPKColName() + " FROM " + _model.householdObs.getAncestorsTableName() + NL +
        ")";
    assertEquals(expectedWithClause, withClause);
  }

  @Test
  @DisplayName("Test creating a WITH clause with filters")
  void testWithClause() {
    
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    filters.add(_model.obsFavNewYearsFilter);
    filters.add(_model.obsBirthDateFilter);
    filters.add(_model.obsMoodFilter);
    filters.add(_model.obsFavNumberFilter);
    filters.add(_model.houseRoofFilter);
    String withClause = StudySubsettingUtils.generateWithClause(_model.observation, filters);
 
    List<String> selectColsList = new ArrayList<>();
    selectColsList.add("a." + _model.observation.getPKColName());
    for (String name : _model.observation.getAncestorPkColNames()) selectColsList.add("a." + name);
    String selectCols = String.join(", ", selectColsList);

    //  SELECT a.household_id, a.participant_id, t.observation_id
    //  FROM Obs_tall t, Obs_ancestors a

    String obsBase = "  SELECT " + selectCols + NL +
        "  FROM " + _model.observation.getTallTableName() + " t, " +
        _model.observation.getAncestorsTableName() + " a" + NL +
        "  WHERE t." + _model.observation.getPKColName() + " = a." + _model.observation.getPKColName() + NL;
    
    String expectedWithClause = _model.observation.getWithClauseName() +  " as (" + NL +
        obsBase + 
        "  AND " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.weight.getId() + "'" + NL +
        "  AND " + NUMBER_VALUE_COL_NAME + " >= 10 AND " + NUMBER_VALUE_COL_NAME + " <= 20" + NL +
        "INTERSECT" + NL +
        obsBase +
        "  AND " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.favNewYears.getId() + "'" + NL +
        "  AND " + DATE_VALUE_COL_NAME + " IN (TO_DATE('2019-03-21T00:00:00', 'YYYY-MM-DD\"T\"HH24:MI:SS'), TO_DATE('2019-03-28T00:00:00', 'YYYY-MM-DD\"T\"HH24:MI:SS'), TO_DATE('2019-06-12T00:00:00', 'YYYY-MM-DD\"T\"HH24:MI:SS'))" + NL +
        "INTERSECT" + NL +
        obsBase + 
        "  AND " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.birthDate.getId() + "'" + NL +
        "  AND " + DATE_VALUE_COL_NAME + " >= TO_DATE('2019-03-21T00:00:00', 'YYYY-MM-DD\"T\"HH24:MI:SS') AND " + DATE_VALUE_COL_NAME + " <= TO_DATE('2019-03-28T00:00:00', 'YYYY-MM-DD\"T\"HH24:MI:SS')" + NL +
        "INTERSECT" + NL +
        obsBase + 
        "  AND " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.mood.getId() + "'" + NL +
        "  AND " + STRING_VALUE_COL_NAME + " IN ('happy', 'jolly', 'giddy')" + NL +
        "INTERSECT" + NL +
        obsBase + 
        "  AND " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.favNumber.getId() + "'" + NL +
        "  AND " + NUMBER_VALUE_COL_NAME + " IN (5, 7, 9 )" + NL +
        ")";
    assertEquals(expectedWithClause, withClause);
  }

  @Test
  @DisplayName("Test creating a select clause for tabular report")
  void testGenerateTabularSelectClause() {
    
    String selectClause = StudySubsettingUtils.generateTabularSelectClause(_model.observation, "a");
    String expectedSelectClause = "SELECT a." + _model.observation.getPKColName() +
        ", a." + _model.participant.getPKColName() +
        ", a." + _model.household.getPKColName() +
        ", " + TT_VARIABLE_ID_COL_NAME + ", " + STRING_VALUE_COL_NAME + ", " + NUMBER_VALUE_COL_NAME + ", " + DATE_VALUE_COL_NAME;
    assertEquals(expectedSelectClause, selectClause);
  }

  @Test
  @DisplayName("Test getting full ancestor PKs list")
  void testGetFullAncestorPKs() {
    Set<String> cols = new HashSet<>(_model.observation.getAncestorSelectPkColNames());
    Set<String> expected = new HashSet<>(Arrays.asList(_model.household.getSelectPKColName(), _model.participant.getSelectPKColName()));
    assertEquals(expected, cols);
  }
  
  @Test
  @DisplayName("Test populating ancestors")
  void testPopulateAncestors() {
    Entity e = _model.study.getEntity(_model.household.getId()).orElse(null);
    assert e != null;
    assertEquals(new ArrayList<Entity>(), e.getAncestorEntities());
    
    e = _model.study.getEntity(_model.participant.getId()).orElse(null);
    List<Entity> l = Collections.singletonList(_model.household);
    assert e != null;
    assertEquals(l, e.getAncestorEntities());

    e = _model.study.getEntity(_model.householdObs.getId()).orElse(null);
    l = Collections.singletonList(_model.household);
    assert e != null;
    assertEquals(l, e.getAncestorEntities());

    e = _model.study.getEntity(_model.observation.getId()).orElse(null);
    l = Arrays.asList(_model.participant, _model.household);
    assert e != null;
    assertEquals(l, e.getAncestorEntities());
  
    e = _model.study.getEntity(_model.sample.getId()).orElse(null);
    l = Arrays.asList(_model.observation, _model.participant, _model.household );
    assert e != null;
    assertEquals(l, e.getAncestorEntities());
  
    e = _model.study.getEntity(_model.treatment.getId()).orElse(null);
    l = Arrays.asList(_model.observation, _model.participant, _model.household);
    assert e != null;
    assertEquals(l, e.getAncestorEntities());
  
  }
  
  @Test
  @DisplayName("Test creating a where clause for tabular report")
  void testGenerateTabularWhereClause() {

    List<Variable> vars = Arrays.asList(_model.birthDate, _model.favNumber);
    List<VariableSpecification> varSpecs = vars.stream()
        .map(v -> new VariableSpecification(v, null, null))
        .collect(Collectors.toList());
    String where = StudySubsettingUtils.generateTabularWhereClause(varSpecs, _model.observation.getPKColName());
    String expected = " WHERE (" + NL +
        " " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.birthDate.getId() + "' OR" + NL +
        " " + TT_VARIABLE_ID_COL_NAME + " = '" + _model.favNumber.getId() + "'" + NL +
        ")" + NL;

    assertEquals(expected, where);
  }

  @Test
  @DisplayName("Test creating an IN clause")
  void testGenerateInClauseClause() {
    
    // construct pruned tree with a pivot (H, HO, O)
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    Entity outputEntity = _model.householdObs;
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, outputEntity);

    List<String> from = Arrays.asList(_model.household.getWithClauseName(), _model.householdObs.getWithClauseName(), _model.observation.getWithClauseName());
    String inClause = StudySubsettingUtils.generateInClause(prunedTree, outputEntity, "t");
    String expected = "AND t." + _model.householdObs.getPKColName() + " IN (" + NL +
        "  SELECT " + _model.householdObs.getSelectPKColName() + NL +
        "  FROM " + String.join(", ", from) + NL +
        "  WHERE " + _model.household.getSelectPKColName() + " = " + _model.householdObs.getWithClauseName() + "." + _model.household.getPKColName() + NL +
        "  AND " + _model.household.getSelectPKColName() + " = " + _model.observation.getWithClauseName() + "." + _model.household.getPKColName() + NL +
        ")";

    assertEquals(expected, inClause);
  }

  @Test
  @DisplayName("Test getting full tabular sql")
  void testGetTabularSql() {
    
    List<Filter> filters = getSomeFilters();

    List<VariableSpecification> outputVariables = Arrays.asList(
            new VariableSpecification(_model.networth, null, null),
            new VariableSpecification( _model.shoesize, null, null));

    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, _model.participant);

    String sql = StudySubsettingUtils.generateTabularSql(outputVariables, _model.participant, filters, prunedTree);
    assertNotEquals("", sql);
    System.out.println("Tabular SQL:" + "\n" + sql);
  }

  @Test
  @DisplayName("Test getting full distribution sql")
  void testGetDistributionSql() {
    
    List<Filter> filters = getSomeFilters();
    
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, _model.participant);

    String sql = StudySubsettingUtils.generateDistributionSql(_model.participant, _model.shoesize, filters, prunedTree);
    assertNotEquals("", sql);
    System.out.println("Distribution SQL:" + "\n" + sql);
  }
  
  @Test
  @DisplayName("Test getting count of entities sql")
  void testGetEntitiesCountSql() {
    
    List<Filter> filters = getSomeFilters();
    
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, _model.participant);

    String sql = StudySubsettingUtils.generateEntityCountSql(_model.participant, filters, prunedTree);
    assertNotEquals("", sql);
    //System.out.println("Entity Count SQL:" + "\n" + sql);
  }

  @Test
  @DisplayName("Test getting count of entities that have a value for a variable sql")
  void testGetVariableCountSql() {
    
    List<Filter> filters = getSomeFilters();
    
    TreeNode<Entity> prunedTree = StudySubsettingUtils.pruneTree(_model.study.getEntityTree(), filters, _model.participant);

    String sql = StudySubsettingUtils.generateVariableCountSql(_model.participant, _model.networth, filters, prunedTree);
    assertNotEquals("", sql);
    //System.out.println("Variable Count SQL:" + "\n" + sql);
  }

  @Test
  @DisplayName("Test get entity count - no filters") 
  void testEntityCountNoFiltersFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    List<Filter> filters = Collections.emptyList();

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(study.getEntityTree(), filters, entity);
    Integer count = StudySubsettingUtils.getEntityCount(_dataSource, prunedEntityTree, entity, new ArrayList<>());
    
    assertEquals(4, count);
  }

  @Test
  @DisplayName("Test get entity count - with filters") 
  void testEntityCountFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    List<Filter> filters = new ArrayList<>();
    filters.add(_filtersForTesting.partHairFilter);
    filters.add(_filtersForTesting.houseObsWaterSupplyFilter);

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(study.getEntityTree(), filters, entity);
    int count = StudySubsettingUtils.getEntityCount(_dataSource, prunedEntityTree, entity, filters);
    
    assertEquals(2, count);
  }

  @Test
  @DisplayName("Test get tabular report - no filters") 
  void testTabularReportNoFiltersFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    List<VariableSpecification> variables = new ArrayList<>();
    variables.add(new VariableSpecification(entity.getVariable("var_17").orElseThrow(), null, null)); // hair color
    variables.add(new VariableSpecification(entity.getVariable("var_20").orElseThrow(), null, null)); // name

    List<Filter> filters = Collections.emptyList();
    
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    StudySubsettingUtils.produceTabularSubset(_dataSource, study, entity,
        variables, filters, outStream);
    String[] expected = {
    "GEMS_Part.Prtcpnt_stable_id.null.null", "GEMS_House.Hshld_stable_id.null.null", "GEMS_Part.var_17.null.null",  "GEMS_Part.var_20.null.null",
    "201", "101",     "blond",   "Martin",
    "202", "101",     "blond",   "Abe",
    "203", "102",     "brown",   "Gladys",
    "204", "102",     "silver",  "Susan"};
 
    assertArrayEquals(expected, outStream.toString().split("\\s+"));
  }

  @Test
  @DisplayName("Test get tabular report - with filters") 
  void testTestTabularReportFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    List<VariableSpecification> variables = new ArrayList<>();
    variables.add(new VariableSpecification(entity.getVariable("var_17").orElseThrow(), null, null)); // hair color
    variables.add(new VariableSpecification(entity.getVariable("var_20").orElseThrow(), null, null)); // name

    List<Filter> filters = new ArrayList<>();
    filters.add(_filtersForTesting.partHairFilter);
    filters.add(_filtersForTesting.houseObsWaterSupplyFilter);

    ByteArrayOutputStream outStream = new ByteArrayOutputStream();

    StudySubsettingUtils.produceTabularSubset(_dataSource, study, entity,
        variables, filters, outStream);
    String[] expected = {
    "GEMS_Part.Prtcpnt_stable_id.null.null", "GEMS_House.Hshld_stable_id.null.null", "GEMS_Part.var_17.null.null",  "GEMS_Part.var_20.null.null",
    "201", "101",     "blond",   "Martin",
    "202", "101",     "blond",   "Abe",
//    "203", "102",     "brown",   "Gladys",
//    "204", "102",     "silver",  "Susan"
    };
    
    assertArrayEquals(expected, outStream.toString().split("\\s+"));
  }
  @Test
  @DisplayName("Test get variable count - no filters") 
  void testVariableCountNoFiltersFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_17";
    Variable var = entity.getVariable(varId).orElseThrow();

    List<Filter> filters = Collections.emptyList();

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(study.getEntityTree(), filters, entity);

    Integer count = StudySubsettingUtils.getVariableCount(_dataSource, prunedEntityTree, entity, var, filters);
    
    assertEquals(4, count);
  }

  @Test
  @DisplayName("Test get variable count - with filters") 
  void testVariableCountFromDb() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_17";
    Variable var = entity.getVariable(varId).orElseThrow();

    List<Filter> filters = new ArrayList<>();
    filters.add(_filtersForTesting.partHairFilter);
    filters.add(_filtersForTesting.houseObsWaterSupplyFilter);

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(study.getEntityTree(), filters, entity);

    Integer count = StudySubsettingUtils.getVariableCount(_dataSource, prunedEntityTree, entity, var, filters);
    
    assertEquals(2, count);
  }

  @Test
  @DisplayName("Test variable distribution - no filters") 
  void testVariableDistributionNoFilters() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_17";
    Variable var = entity.getVariable(varId).orElseThrow();

    List<Filter> filters = Collections.emptyList();

    Map<String, Integer> expectedDistribution = new HashMap<>(){{
      put("blond", 2);
      put("brown", 1);
      put("silver", 1);
    }};

    testDistributionResponse(study, entity, var, filters, expectedDistribution);
  }

  @Test
  @DisplayName("Test variable distribution - with filters") 
  void testVariableDistribution() {
    
    Study study = Study.loadStudy(_dataSource, LoadStudyTest.STUDY_ID);

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_17";
    Variable var = entity.getVariable(varId).orElseThrow();
    
    List<Filter> filters = new ArrayList<>();
    filters.add(_filtersForTesting.houseCityFilter);
    filters.add(_filtersForTesting.houseObsWaterSupplyFilter);

    Map<String, Integer> expectedDistribution = new HashMap<>(){{
      put("brown", 1);
      put("silver", 1);
    }};

    testDistributionResponse(study, entity, var, filters, expectedDistribution);
  }

  private void testDistributionResponse(Study study, Entity entity, Variable var, List<Filter> filters, Map<String, Integer> expectedDistribution) {

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(study.getEntityTree(), filters, entity);

    Stream<TwoTuple<String,Integer>> distributionStream = StudySubsettingUtils.produceVariableDistribution(
        _dataSource, prunedEntityTree, entity, var, filters);

    Map<String,Integer> result = Functions.getMapFromList(IteratorUtil.toIterable(distributionStream.iterator()), tuple -> tuple);

    assertEquals(result, expectedDistribution);
  }

  List<Filter> getSomeFilters() {
    List<Filter> filters = new ArrayList<>();
    filters.add(_model.obsWeightFilter);
    filters.add(_model.obsFavNewYearsFilter);
    filters.add(_model.obsBirthDateFilter);
    filters.add(_model.obsMoodFilter);
    filters.add(_model.obsFavNumberFilter);
    filters.add(_model.houseRoofFilter);
    filters.add(_model.houseObsWaterSupplyFilter);
    return filters;
  }

  @Test
  @DisplayName("Test tabular results without vars containing no nulls")
  void testTabularResultsNoVars() {
    testTabularResults(Arrays.asList());
  }

  @Test
  @DisplayName("Test tabular results with vars containing data for all rows")
  void testTabularResultsVarsWithData() {
    testTabularResults(Arrays.asList(_model.haircolor, _model.shoesize));
  }

  @Test
  @DisplayName("Test tabular results with vars containing missing data for one var")
  void testTabularResultsVarsWithMissingData() {
    testTabularResults(Arrays.asList(_model.networth, _model.shoesize));
  }

  @Test
  @DisplayName("Test tabular results with vars containing missing data for some rows")
  void testTabularResultsVarsWithPartialData() {
    testTabularResults(Arrays.asList(_model.earsize, _model.shoesize));
  }

  private void testTabularResults(List<Variable> requestedVars) {

    List<VariableSpecification> varSpecs = requestedVars.stream()
        .map(v -> new VariableSpecification(v, null, null))
        .collect(Collectors.toList());
    Entity entity = _model.participant;
    List<Map<String,String>> results = getTabularOutputRows(entity, varSpecs);

    for (Map<String,String> row : results) {
      LOG.info(FormatUtil.prettyPrint(row, FormatUtil.Style.SINGLE_LINE));
    }
    // test number of results; should always be the same regardless of var data (PKs always provided)
    assertEquals(4, results.size());

    Map<String,String> firstRow = results.iterator().next();

    // make sure results have cols for entity PK and ancestor PKs
    assertTrue(firstRow.containsKey(toDotNotation(new PkVariableDef(entity.getId(), entity.getPKColName()))));
    for (Entity ancestor : entity.getAncestorEntities()) {
      assertTrue(firstRow.containsKey(toDotNotation(new PkVariableDef(ancestor.getId(), ancestor.getPKColName()))));
    }

    // make sure results have cols for all requested rows
    for (VariableSpecification var : varSpecs) {
      assertTrue(firstRow.containsKey(toDotNotation(var)));
    }
  }

  private List<Map<String,String>> getTabularOutputRows(Entity entity, List<VariableSpecification> varSpecs) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    StudySubsettingUtils.produceTabularSubset(_dataSource, _model.study, entity, varSpecs, Collections.emptyList(), buffer);
    Scanner scanner = new Scanner(buffer.toString());
    if (!scanner.hasNextLine()) {
      throw new RuntimeException("Tabular output did not contain a header row.");
    }
    DelimitedDataParser parser = new DelimitedDataParser(scanner.nextLine(), TAB, true);
    List<Map<String,String>> rows = new ArrayList<>();
    while(scanner.hasNextLine()) {
      rows.add(parser.parseLine(scanner.nextLine()));
    }
    return rows;
  }
}
