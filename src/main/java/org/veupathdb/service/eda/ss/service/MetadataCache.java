package org.veupathdb.service.eda.ss.service;

import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataCache implements StudyProvider {

  // singleton pattern
  private static final MetadataCache _instance = new MetadataCache();
  public static MetadataCache instance() { return _instance; }

  // instance fields
  private List<StudyOverview> _studyOverviews;  // cache the overviews
  private final Map<String, Study> _studies = new HashMap<>(); // cache the studies

  @Override
  public synchronized Study getStudyById(String studyId) {
    return _studies.computeIfAbsent(studyId,
        id -> new StudyFactory(
            Resources.getApplicationDataSource(),
            Resources.getAppDbSchema(),
            false,
            Resources.getConvertAssaysFlag()
        ).getStudyById(id));
  }

  @Override
  public synchronized List<StudyOverview> getStudyOverviews() {
    if (_studyOverviews == null) {
      _studyOverviews = new StudyFactory(
          Resources.getApplicationDataSource(),
          Resources.getAppDbSchema(),
          false,
          Resources.getConvertAssaysFlag()
      ).getStudyOverviews();
    }
    return Collections.unmodifiableList(_studyOverviews);
  }

  public synchronized void clear() {
    _studyOverviews = null;
    _studies.clear();
  }
}

