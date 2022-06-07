package org.veupathdb.service.eda.ss.service;

import jakarta.ws.rs.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
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
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.test.MockModel;
import org.veupathdb.service.eda.ss.test.StubDb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StudiesTest {

  private static MockModel _model;
  private static Study _study;

  @BeforeAll
  public static void setUp() {
    _model = new MockModel();
    DataSource dataSource = StubDb.getDataSource();
    _study = new StudyFactory(dataSource, StubDb.APP_DB_SCHEMA, false, StubDb.ASSAY_CONVERSION_FLAG).getStudyById("DS-2324");
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
    stringFilter.setStringSet(List.of("a", "b", "c"));
    afs.add(stringFilter);
    
    APINumberRangeFilter numberFilter = new APINumberRangeFilterImpl();
    numberFilter.setEntityId(_model.observation.getId());
    numberFilter.setVariableId(_model.weight.getId());
    numberFilter.setMin(0);
    numberFilter.setMax(10);
    afs.add(numberFilter);
    
    RequestBundle.constructFiltersFromAPIFilters(_model.study, afs, StubDb.APP_DB_SCHEMA);
    
    assertEquals(2, afs.size());
  }

  @Test
  @DisplayName("Test rejection of invalid API filters")
  void testIncorrectConstructFilters() {
    
    assertThrows(BadRequestException.class, () -> {
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

      RequestBundle.constructFiltersFromAPIFilters(_model.study, afs, StubDb.APP_DB_SCHEMA);
    });
  }

}
