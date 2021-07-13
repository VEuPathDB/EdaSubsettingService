package org.veupathdb.service.eda.ss.model.distribution;

import java.util.List;
import org.gusdb.fgputil.Tuples;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramStats;

public class DistributionResult extends Tuples.TwoTuple<List<HistogramBin>, HistogramStats> {

  public DistributionResult(List<HistogramBin> histogram, HistogramStats stats) {
    super(histogram, stats);
  }

  public List<HistogramBin> getHistogramData() {
    return getFirst();
  }

  public HistogramStats getStatistics() {
    return getSecond();
  }

}
