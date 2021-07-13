package org.veupathdb.service.eda.ss.model.distribution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramStats;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest.ValueSpecType;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.VariableWithValues;
import org.veupathdb.service.eda.ss.model.distribution.BinHelpers.Bin;
import org.veupathdb.service.eda.ss.model.filter.Filter;

public abstract class AbstractBinDistribution<T extends VariableWithValues, S, R extends Bin<S>> extends AbstractDistribution<T> {

  protected enum ValueSource { CONFIG, DB }

  protected abstract class StatsCollector {
    abstract void accept(S value, Long count);
    abstract HistogramStats toHistogramStats(int uniqueEntityCount);
  }

  private final S _displayMin;
  private final S _displayMax;

  protected abstract S getTypedObject(String objectName, Object value, ValueSource source);
  protected abstract StatsCollector getStatsCollector();
  protected abstract R getFirstBin();
  protected abstract R getNextBin(R currentBin);
  protected abstract List<HistogramBin> getExtraBins(R currentBin);

  public AbstractBinDistribution(DataSource ds, Study study, Entity targetEntity,
                                 T variable, List<Filter> filters, ValueSpecType valueSpec,
                                 Object displayMin, Object displayMax) {
    super(ds, study, targetEntity, variable, filters, valueSpec);
    _displayMin = getTypedObject("displayMin", displayMin, ValueSource.CONFIG);
    _displayMax = getTypedObject("displayMax", displayMax, ValueSource.CONFIG);
  }

  @Override
  protected DistributionResult processDistributionStream(Stream<TwoTuple<String, Long>> distributionStream, int uniqueEntityCount) {
    StatsCollector stats = getStatsCollector();
    R currentBin = getFirstBin();
    List<HistogramBin> bins = new ArrayList<>();
    for (TwoTuple<String,Long> tuple : IteratorUtil.toIterable(distributionStream.iterator())) {
      S value = getTypedObject("value", tuple.getKey(), ValueSource.DB);
      stats.accept(value, tuple.getValue());
      while (!currentBin.accept(value, tuple.getValue())) {
        bins.add(currentBin.toHistogramBin());
        currentBin = getNextBin(currentBin);
      }
    }
    bins.add(currentBin.toHistogramBin());
    bins.addAll(getExtraBins(currentBin));
    return new DistributionResult(bins, stats.toHistogramStats(uniqueEntityCount));
  }

}
