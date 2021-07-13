package org.veupathdb.service.eda.ss.model.distribution;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramStats;
import org.veupathdb.service.eda.generated.model.HistogramStatsImpl;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest.ValueSpecType;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.NumberVariable;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.distribution.BinHelpers.NumberBin;
import org.veupathdb.service.eda.ss.model.filter.Filter;

public class NumberBinDistribution extends AbstractBinDistribution<NumberVariable, Double, NumberBin> {

  private final double _binWidth;

  public NumberBinDistribution(DataSource ds, Study study, Entity targetEntity, NumberVariable var,
                               List<Filter> filters, ValueSpecType valueSpec, BinSpec binSpec) {
    super(ds, study, targetEntity, var, filters, valueSpec, binSpec.getDisplayRangeMin(), binSpec.getDisplayRangeMax());
    _binWidth = binSpec.getBinWidth().doubleValue();
  }

  @Override
  protected Double getTypedObject(String objectName, Object value, ValueSource source) {
    Supplier<RuntimeException> exSupplier = () -> switch(source) {
      case CONFIG -> new BadRequestException(objectName + " must be a number value.");
      case DB -> new RuntimeException("Value in column " + objectName + " is not a number.");
    };
    if (value instanceof Number) {
      return ((Number)value).doubleValue();
    }
    if (value instanceof String) {
      try {
        return Double.parseDouble((String)value);
      }
      catch (NumberFormatException e) {
        throw exSupplier.get();
      }
    }
    throw exSupplier.get();
  }

  @Override
  protected StatsCollector getStatsCollector() {
    return new StatsCollector() {

      private Double _subsetMin;
      private Double _subsetMax;
      private long _numValues = 0;
      private double _sumOfValues = 0;

      @Override
      void accept(Double value, Long count) {
        if (_subsetMin == null) _subsetMin = value;
        _subsetMax = value;
        _numValues += count;
        _sumOfValues += (count * value);
      }

      @Override
      HistogramStats toHistogramStats(int uniqueEntityCount) {
        HistogramStats stats = new HistogramStatsImpl();
        stats.setSubsetMin(_subsetMin);
        stats.setSubsetMax(_subsetMax);
        stats.setSubsetMean(_sumOfValues / _numValues);
        stats.setNumVarValues((int)_numValues); // FIXME: int cast ok here?
        stats.setNumDistinctEntityRecords(uniqueEntityCount);
        stats.setNumMissingCases(0); // FIXME: get from null tuple?
        return stats;
      }
    };
  }

  @Override
  protected NumberBin getFirstBin() {
    double start = Math.min(
        _variable.getDisplayRangeMin().doubleValue(),
        _variable.getRangeMin().doubleValue());
    return new NumberBin(start, start + _binWidth);
  }

  @Override
  protected NumberBin getNextBin(NumberBin currentBin) {
    return new NumberBin(currentBin._end, currentBin._end + _binWidth);
  }

  @Override
  protected List<HistogramBin> getExtraBins(NumberBin currentBin) {
    double end = Math.max(
        _variable.getDisplayRangeMax().doubleValue(),
        _variable.getRangeMax().doubleValue());
    List<HistogramBin> extraBins = new ArrayList<>();
    while (currentBin._end < end) {
      currentBin = new NumberBin(currentBin._end, currentBin._end + _binWidth);
      extraBins.add(currentBin.toHistogramBin());
    }
    return extraBins;
  }

}
