package org.veupathdb.service.eda.ss.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;
import org.veupathdb.service.eda.ss.model.db.VariableFactory;
import org.veupathdb.service.eda.ss.model.reducer.MetadataFileBinaryProvider;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MetadataCache implements StudyProvider {
  private static final Logger LOG = LogManager.getLogger(MetadataCache.class);

  // instance fields
  private final BinaryFilesManager _binaryFilesManager;
  private final StudyProvider _sourceStudyProvider;
  private List<StudyOverview> _studyOverviews;  // cache the overviews
  private final Map<String, Study> _studies = new HashMap<>(); // cache the studies
  private final Map<String, Boolean> _studyHasFilesCache = new HashMap<>();
  private final ScheduledExecutorService _scheduledThreadPool = Executors.newScheduledThreadPool(1); // Shut this down.

  public MetadataCache() {
    _binaryFilesManager = Resources.getBinaryFilesManager();
    _sourceStudyProvider = getCuratedStudyFactory();
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L, 5L, TimeUnit.MINUTES);
  }

  MetadataCache(BinaryFilesManager binaryFilesManager,
                        StudyProvider sourceStudyProvider,
                        Duration refreshInterval) {
    _binaryFilesManager = binaryFilesManager;
    _sourceStudyProvider = sourceStudyProvider;
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L,
        refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
  }

  @Override
  public synchronized Study getStudyById(String studyId) {
    return _studies.computeIfAbsent(studyId,
        id -> getCuratedStudyFactory().getStudyById(id));
  }

  @Override
  public synchronized List<StudyOverview> getStudyOverviews() {
    if (_studyOverviews == null) {
      _studyOverviews = _sourceStudyProvider.getStudyOverviews();
    }
    return Collections.unmodifiableList(_studyOverviews);
  }

  private synchronized boolean studyHasFiles(String studyId) {
    _studyHasFilesCache.computeIfAbsent(studyId, _binaryFilesManager::studyHasFiles);
    return _studyHasFilesCache.get(studyId);
  }

  private StudyProvider getCuratedStudyFactory() {
    return new StudyFactory(
        Resources.getApplicationDataSource(),
        Resources.getAppDbSchema(),
        StudyOverview.StudySourceType.CURATED,
        new VariableFactory(
            Resources.getApplicationDataSource(),
            Resources.getAppDbSchema(),
            new MetadataFileBinaryProvider(_binaryFilesManager),
            this::studyHasFiles)
        );
  }

  public synchronized void clear() {
    _studyOverviews = null;
    _studies.clear();
  }

  public void shutdown() {
    _scheduledThreadPool.shutdown();
  }

  private void invalidateOutOfDateStudies() {
    LOG.info("Checking which studies are out of date in cache.");
    List<StudyOverview> dbStudies = _sourceStudyProvider.getStudyOverviews();
    boolean studyOverviewsOutOfDate = _studyOverviews != null && _studyOverviews.stream().anyMatch(study -> isOutOfDate(study, dbStudies));
    List<Study> studiesToRemove = _studies.values().stream()
        .filter(study -> isOutOfDate(study, dbStudies))
        .toList();
    synchronized (this) {
      LOG.info("Removing the following out of date or missing studies from cache: "
          + studiesToRemove.stream().map(StudyOverview::getStudyId).collect(Collectors.joining(",")));
      dbStudies.forEach(study -> _studyHasFilesCache.put(study.getStudyId(), _binaryFilesManager.studyHasFiles(study.getStudyId())));
      if (studyOverviewsOutOfDate) {
        _studyOverviews = null;
      }
      _studies.entrySet().removeIf(study ->
          studiesToRemove.stream().anyMatch(removeStudy -> removeStudy.getStudyId().equals(study.getKey())));
    }
  }

  private boolean isOutOfDate(StudyOverview studyOverview, List<StudyOverview> dbStudies) {
    Optional<StudyOverview> matchingDbStudy = dbStudies.stream()
        .filter(dbStudy -> dbStudy.getStudyId().equals(studyOverview.getStudyId()))
        .findAny();
    // Study not in DB anymore, remove it from cache.
    if (matchingDbStudy.isEmpty()) {
      return true;
    }
    // If in DB, check if it's out of date.
    return matchingDbStudy.get().getLastModified().after(studyOverview.getLastModified());
  }
}

