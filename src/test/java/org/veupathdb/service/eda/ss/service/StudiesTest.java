package org.veupathdb.service.eda.ss.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.sql.DataSource;
import jakarta.ws.rs.InternalServerErrorException;
import org.gusdb.fgputil.distribution.DistributionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.veupathdb.service.eda.generated.model.APIEntity;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APIFilterImpl;
import org.veupathdb.service.eda.generated.model.APINumberRangeFilter;
import org.veupathdb.service.eda.generated.model.APINumberRangeFilterImpl;
import org.veupathdb.service.eda.generated.model.APIStringSetFilter;
import org.veupathdb.service.eda.generated.model.APIStringSetFilterImpl;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.FiltersForTesting;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.TestModel;
import org.veupathdb.service.eda.ss.model.distribution.DistributionFactory;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.stubdb.StubDb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StudiesTest {

  private static TestModel _model;
  private static DataSource _dataSource;
  private static FiltersForTesting _filtersForTesting;
  private static Study _study;

  @BeforeAll
  public static void setUp() {
    _model = new TestModel();
    _dataSource = StubDb.getDataSource();
    _study = Study.loadStudy(_dataSource, "DS-2324");
    _filtersForTesting = new FiltersForTesting(_study);
  }

  @Test
  @DisplayName("Test entity tree to api tree")
  void testEntityTreeToAPITree() {

    APIEntity apiEntityTree = ApiConversionUtil.entityTreeToAPITree(_study.getEntityTree());
    assertEquals("GEMS_House", apiEntityTree.getId());

    assertEquals(2, apiEntityTree.getChildren().size());
  }

  @Test
  @DisplayName("Test get study details")
  void testGetStudyDetails() {
    APIStudyDetail studyDetail = ApiConversionUtil.getApiStudyDetail(_study);
    assertNotNull(studyDetail);
  }

  @Test
  @DisplayName("Test valid construction of filters from API filters")
  void testConstructFilters() {
    
    List<APIFilter> afs = new ArrayList<>();
    
    APIStringSetFilter stringFilter = new APIStringSetFilterImpl();
    stringFilter.setEntityId(_model.participant.getId());
    stringFilter.setVariableId(_model.earsize.getId());
    
    afs.add(stringFilter);
    
    APINumberRangeFilter numberFilter = new APINumberRangeFilterImpl();
    numberFilter.setEntityId(_model.observation.getId());
    numberFilter.setVariableId(_model.weight.getId());
    afs.add(numberFilter);
    
    RequestBundle.constructFiltersFromAPIFilters(_model.study, afs);
    
    assertEquals(2, afs.size());
  }

  @Test
  @DisplayName("Test rejection of invalid API filters")
  void testIncorrectConstructFilters() {
    
    assertThrows(InternalServerErrorException.class, () -> {
      List<APIFilter> afs = new ArrayList<>();

      // a legit filter
      APIStringSetFilter stringFilter = new APIStringSetFilterImpl();
      stringFilter.setEntityId(_model.participant.getId());
      stringFilter.setVariableId(_model.earsize.getId());

      afs.add(stringFilter);

      // an invalid one... can't be the superclass
      APIFilter nakedFilter = new APIFilterImpl();
      nakedFilter.setEntityId(_model.observation.getId());
      afs.add(nakedFilter);

      RequestBundle.constructFiltersFromAPIFilters(_model.study, afs);
    });
  }

  @Test
  @DisplayName("Test variable distribution - no filters")
  void testVariableDistributionNoFilters() throws IOException {

    Study study = Study.loadStudy(Resources.getApplicationDataSource(), "DS-2324");

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_p4";
    VariableWithValues var = (VariableWithValues)entity.getVariable(varId).orElseThrow();

    List<Filter> filters = Collections.emptyList();

    int expectedVariableCount = 4;

    Map<String, Integer> expectedDistribution = new HashMap<>(){{
      put("blond", 2);
      put("brown", 1);
      put("silver", 1);
    }};

    testDistributionResponse(study, entity, var, filters, expectedVariableCount, expectedDistribution);
  }

  @Test
  @DisplayName("Test variable distribution - with filters")
  void testVariableDistribution() throws IOException {

    Study study = Study.loadStudy(Resources.getApplicationDataSource(), "DS-2324");

    String entityId = "GEMS_Part";
    Entity entity = study.getEntity(entityId).orElseThrow();

    String varId = "var_p4";
    VariableWithValues var = (VariableWithValues)entity.getVariable(varId).orElseThrow();

    List<Filter> filters = new ArrayList<>();
    filters.add(_filtersForTesting.houseCityFilter);
    filters.add(_filtersForTesting.houseObsWaterSupplyFilter);

    int expectedVariableCount = 2;

    Map<String, Integer> expectedDistribution = new HashMap<>(){{
      put("brown", 1);
      put("silver", 1);
    }};

    testDistributionResponse(study, entity, var, filters, expectedVariableCount, expectedDistribution);
  }

  private void testDistributionResponse(Study study, Entity entity, VariableWithValues var,
      List<Filter> filters, int expectedVariableCount, Map<String, Integer> expectedDistribution) throws IOException {

    DistributionResult result = DistributionFactory.processDistributionRequest(
        _dataSource, study, entity, var, filters, ValueSpec.COUNT, Optional.empty());

    // check variable count
    assertEquals(expectedVariableCount, result.getStatistics().getNumDistinctEntityRecords());

    List<HistogramBin> responseRows = ApiConversionUtil.toApiHistogramBins(result.getHistogramData());

    // check number of distribution rows
    assertEquals(expectedDistribution.size(), responseRows.size());

    for (Map.Entry<String,Integer> expectedRow : expectedDistribution.entrySet()) {
      // find row in list
      HistogramBin bin = responseRows.stream().filter(b -> b.getBinLabel().equals(expectedRow.getKey())).findFirst()
          .orElseThrow(() -> new RuntimeException("expected bin row '" + expectedRow.getKey() + "' not found in result"));
      int count = bin.getValue().intValue(); // will throw if not integer
      // check distribution size for key
      assertEquals(expectedRow.getValue(), count);
    }
  }

}
