package org.veupathdb.service.eda.ss.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.veupathdb.service.eda.generated.model.APIStudyOverview;
import org.veupathdb.service.eda.generated.model.APIStudyOverviewImpl;
import org.veupathdb.service.eda.ss.Resources;

public class MetadataCache {

  private static Map<String, APIStudyOverview> apiStudyOverviews;  // cache the overviews
  private static Map<String, Study> studies = new ConcurrentHashMap<>(); // cache the studies

  public static Study getStudy(String studyId){
    return studies.computeIfAbsent(studyId,
        id -> Study.loadStudy(Resources.getApplicationDataSource(), id));
  }

  public static List<APIStudyOverview> getStudyOverviews() {
    if (apiStudyOverviews == null) {
      List<Study.StudyOverview> overviews = Study.getStudyOverviews(Resources.getApplicationDataSource());
      Map<String, APIStudyOverview> apiStudyOverviewsTmp = new LinkedHashMap<>();
      for (Study.StudyOverview overview : overviews) {
        APIStudyOverview study = new APIStudyOverviewImpl();
        study.setId(overview.getId());
        study.setDatasetId(overview.getDatasetId());
        apiStudyOverviewsTmp.put(study.getId(), study);
      }
      apiStudyOverviews = apiStudyOverviewsTmp;
    }
    return new ArrayList<>( apiStudyOverviews.values() );
  }

  public static void clear() {
    apiStudyOverviews = null;
    studies.clear();
  }
}
