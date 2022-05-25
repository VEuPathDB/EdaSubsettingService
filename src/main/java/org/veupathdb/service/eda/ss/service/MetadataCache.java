package org.veupathdb.service.eda.ss.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.veupathdb.service.eda.generated.model.APIStudyOverview;
import org.veupathdb.service.eda.generated.model.APIStudyOverviewImpl;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;

public class MetadataCache {

  private static Map<String, APIStudyOverview> apiStudyOverviews;  // cache the overviews
  private static final Map<String, Study> studies = new HashMap<>(); // cache the studies

  public static synchronized Study getStudy(String studyId) {
    return studies.computeIfAbsent(studyId,
        id -> new StudyFactory(
            Resources.getApplicationDataSource(),
            Resources.getAppDbSchema(),
            Resources.getConvertAssaysFlag()
        ).loadStudy(id));
  }

  public static synchronized List<APIStudyOverview> getStudyOverviews() {
    if (apiStudyOverviews == null) {
      apiStudyOverviews = new HashMap<>();
      List<StudyOverview> overviews = new StudyFactory(
          Resources.getApplicationDataSource(),
          Resources.getAppDbSchema(),
          Resources.getConvertAssaysFlag()
      ).getStudyOverviews();
      for (StudyOverview overview : overviews) {
        APIStudyOverview study = new APIStudyOverviewImpl();
        study.setId(overview.getStudyId());
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

