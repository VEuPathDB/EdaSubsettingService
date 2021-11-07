package org.veupathdb.service.eda.ss.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.veupathdb.service.eda.generated.model.APIStudyOverview;
import org.veupathdb.service.eda.generated.model.APIStudyOverviewImpl;
import org.veupathdb.service.eda.ss.Resources;

public class MetadataCache {

  private static Map<String, APIStudyOverview> apiStudyOverviews;  // cache the overviews
  private static Map<String, Study> studies = new HashMap<>(); // cache the studies

  public static synchronized Study getStudy(String studyId) {
    return studies.computeIfAbsent(studyId,
        id -> Study.loadStudy(Resources.getApplicationDataSource(), id));
  }

  public static synchronized List<APIStudyOverview> getStudyOverviews() {
    if (apiStudyOverviews == null) {
      apiStudyOverviews = new HashMap<>();
      List<Study.StudyOverview> overviews = Study.getStudyOverviews(Resources.getApplicationDataSource());
      for (Study.StudyOverview overview : overviews) {
        APIStudyOverview study = new APIStudyOverviewImpl();
        study.setId(overview.getId());
        study.setDatasetId(overview.getDatasetId());
        apiStudyOverviews.put(study.getId(), study);
      }
    }
    return new ArrayList<>(apiStudyOverviews.values());
  }

  public static synchronized void clear() {
    apiStudyOverviews = null;
    studies.clear();
  }
}
