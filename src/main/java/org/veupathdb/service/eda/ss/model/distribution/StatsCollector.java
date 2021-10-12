package org.veupathdb.service.eda.ss.model.distribution;

import org.veupathdb.service.eda.generated.model.HistogramStats;
import org.veupathdb.service.eda.generated.model.HistogramStatsImpl;

public class StatsCollector<T> {

  public static final String NOT_APPLICABLE_VALUE = "N/A";

  private boolean _isDataPresent = false;
  protected T _subsetMin;
  protected T _subsetMax;
  private long _numValues = 0;
  private long _numDistinctValues = 0;

  public void accept(T value, Long count) {
    _isDataPresent = true;
    if (_subsetMin == null) _subsetMin = value;
    _subsetMax = value;
    _numValues += count;
    _numDistinctValues++;
  }

  public HistogramStats toHistogramStats(long subsetEntityCount, long missingCasesCount) {
    HistogramStats stats = new HistogramStatsImpl();
    if (_isDataPresent) {
      stats.setSubsetMin(_subsetMin);
      stats.setSubsetMax(_subsetMax);
    }
    else {
      fillEmptyStats(stats);
    }
    stats.setSubsetSize(subsetEntityCount);
    stats.setNumVarValues(_numValues);
    stats.setNumDistinctValues(_numDistinctValues);
    stats.setNumDistinctEntityRecords(subsetEntityCount - missingCasesCount);
    stats.setNumMissingCases(missingCasesCount);
    return stats;
  }

  protected boolean isDataPresent() {
    return _isDataPresent;
  }

  static void fillEmptyStats(HistogramStats stats) {
    stats.setSubsetMin(NOT_APPLICABLE_VALUE);
    stats.setSubsetMax(NOT_APPLICABLE_VALUE);
    stats.setSubsetMean(NOT_APPLICABLE_VALUE);
  }
}
