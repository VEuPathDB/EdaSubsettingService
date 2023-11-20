package org.veupathdb.service.eda.ss.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MetadataCache implements StudyProvider {
  private static final Logger LOG = LogManager.getLogger(MetadataCache.class);

  // instance fields
  private List<StudyOverview> _studyOverviews;  // cache the overviews
  private final BinaryFilesManager _binaryFilesManager;
  private final Supplier<StudyProvider> _sourceStudyProvider;
  private final Map<String, Study> _studies = new HashMap<>(); // cache the studies
  private final Map<String, Boolean> _studyHasFilesCache = new HashMap<>();
  private final ScheduledExecutorService _scheduledThreadPool = Executors.newScheduledThreadPool(1); // Shut this down.
  private final Optional<CountDownLatch> _appDbReadySignal;

  public MetadataCache(BinaryFilesManager binaryFilesManager, CountDownLatch appDbReadySignal) {
    _binaryFilesManager = binaryFilesManager;
    _sourceStudyProvider = this::getCuratedStudyFactory; // Lazily initialize to ensure database connection is established before construction.
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L, 5L, TimeUnit.MINUTES);
    _appDbReadySignal = Optional.of(appDbReadySignal);
  }

  // Visible for testing
  MetadataCache(BinaryFilesManager binaryFilesManager,
                StudyProvider sourceStudyProvider,
                Duration refreshInterval) {
    _binaryFilesManager = binaryFilesManager;
    _sourceStudyProvider = () -> sourceStudyProvider;
    _scheduledThreadPool.scheduleAtFixedRate(this::invalidateOutOfDateStudies, 0L,
        refreshInterval.toMillis(), TimeUnit.MILLISECONDS);
    _appDbReadySignal = Optional.empty();
  }

  @Override
  public synchronized Study getStudyById(String studyId) {
    return _studies.computeIfAbsent(studyId,
        id -> getCuratedStudyFactory().getStudyById(id));
  }

  @Override
  public synchronized List<StudyOverview> getStudyOverviews() {
    if (_studyOverviews == null) {
      _studyOverviews = _sourceStudyProvider.get().getStudyOverviews();
    }
    return Collections.unmodifiableList(_studyOverviews);
  }

  private synchronized boolean studyHasFiles(String studyAbbrev) {
    LOG.info("Asking if studyHasFiles for " + studyAbbrev + ".  Is cached? " + _studyHasFilesCache.containsKey(studyAbbrev));
    _studyHasFilesCache.computeIfAbsent(studyAbbrev, _binaryFilesManager::studyHasFiles);
    boolean result = _studyHasFilesCache.get(studyAbbrev);
    LOG.info("Result of call for " + studyAbbrev + "? " + result);
    if (studyAbbrev.equals("BRAZIL0001-1") || studyAbbrev.equals("BRAZIL0001_1")) {
      LOG.info("Where???\n" + FormatUtil.getCurrentStackTrace());
    }
    return result;
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
    _studyHasFilesCache.clear();
  }

  public void shutdown() {
    _scheduledThreadPool.shutdown();
  }

  private void invalidateOutOfDateStudies() {
    // Wait until main thread signals that the application database is ready. If we try to access it before, it will
    // not throw an exception, but it will use the internal stub DB implementation which will result in missing studies.
    if (_appDbReadySignal.isPresent()) {
      LOG.info("Awaiting application database to be ready.");
      try {
        _appDbReadySignal.get().await();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.warn("Thread interrupted while awaiting Application database readiness.", e);
        return;
      }
    }

    LOG.info("Checking which studies are out of date in cache.");
    List<StudyOverview> dbStudies = _sourceStudyProvider.get().getStudyOverviews();
    List<Study> studiesToRemove = _studies.values().stream()
        .filter(study -> isOutOfDate(study, dbStudies))
        .toList();
    synchronized (this) {
      LOG.info("Removing the following out of date or missing studies from cache: "
          + studiesToRemove.stream().map(StudyOverview::getStudyId).collect(Collectors.joining(",")));

      // For each study with a study overview, check if the files exist and cache the result.
      dbStudies.forEach(study -> _studyHasFilesCache.put(study.getStudyId(), _binaryFilesManager.studyHasFiles(study.getStudyId())));

      // Replace study overviews with those available in DB.
      _studyOverviews = dbStudies;

      // Remove any studies with full metadata loaded if they have been modified. They will be lazily repopulated.
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

