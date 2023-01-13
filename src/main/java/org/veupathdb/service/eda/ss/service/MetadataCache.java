package org.veupathdb.service.eda.ss.service;

import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;
import org.veupathdb.service.eda.ss.model.db.VariableFactory;
import org.veupathdb.service.eda.ss.model.reducer.MetadataFileBinaryProvider;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MetadataCache implements StudyProvider {
  private static final BinaryFilesManager BINARY_FILES_MANAGER = new BinaryFilesManager(
          new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));

  // singleton pattern
  private static final MetadataCache _instance = new MetadataCache();

  public static MetadataCache instance() { return _instance; }

  // instance fields
  private List<StudyOverview> _studyOverviews;  // cache the overviews
  private final Map<String, Study> _studies = new HashMap<>(); // cache the studies

  @Override
  public synchronized Study getStudyById(String studyId) {
    return _studies.computeIfAbsent(studyId,
        id -> getCuratedStudyFactory().getStudyById(id));
  }

  @Override
  public synchronized List<StudyOverview> getStudyOverviews() {
    if (_studyOverviews == null) {
      _studyOverviews = getCuratedStudyFactory().getStudyOverviews();
    }
    return Collections.unmodifiableList(_studyOverviews);
  }

  private static StudyProvider getCuratedStudyFactory() {
    return new StudyFactory(
        Resources.getApplicationDataSource(),
        Resources.getAppDbSchema(),
        StudyOverview.StudySourceType.CURATED,
        new VariableFactory(
            Resources.getApplicationDataSource(),
            Resources.getAppDbSchema(),
            new MetadataFileBinaryProvider(BINARY_FILES_MANAGER))
    );
  }

  public synchronized void clear() {
    _studyOverviews = null;
    _studies.clear();
  }
}

