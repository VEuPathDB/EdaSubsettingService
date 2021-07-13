package org.veupathdb.service.eda.ss.model.distribution;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.BinUnits;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramStats;
import org.veupathdb.service.eda.generated.model.HistogramStatsImpl;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest.ValueSpecType;
import org.veupathdb.service.eda.ss.model.DateVariable;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.distribution.BinHelpers.Bin;
import org.veupathdb.service.eda.ss.model.distribution.BinHelpers.DateBin;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.service.RequestBundle;

public class DateBinDistribution extends AbstractBinDistribution<DateVariable, LocalDateTime, DateBin> {

  private final TemporalUnit _binUnits;

  public DateBinDistribution(DataSource ds, Study study, Entity targetEntity, DateVariable var,
                             List<Filter> filters, ValueSpecType valueSpec, BinSpec binSpec) {
    super(ds, study, targetEntity, var, filters, valueSpec, binSpec.getDisplayRangeMin(), binSpec.getDisplayRangeMax());
    _binUnits = switch(binSpec.getBinUnits()) {
      case DAY -> ChronoUnit.DAYS;
      case WEEK -> ChronoUnit.WEEKS;
      case MONTH -> ChronoUnit.MONTHS;
      case YEAR -> ChronoUnit.YEARS;
    };
  }

  @Override
  protected LocalDateTime getTypedObject(String objectName, Object value, ValueSource source) {
    if (value instanceof String) {
      return RequestBundle.parseDate((String)value);
    }
    throw switch(source) {
      case CONFIG -> new BadRequestException(objectName + " must be a date string value.");
      case DB -> new RuntimeException("Converted value in column " + objectName + " is not a valid date string.");
    };
  }

  @Override
  protected StatsCollector getStatsCollector() {
    return new StatsCollector() {

      private LocalDateTime _subsetMin;
      private LocalDateTime _subsetMax;
      private long _numValues = 0;
      private long _sumOfValues = 0;

      @Override
      void accept(LocalDateTime value, Long count) {
        if (_subsetMin == null) _subsetMin = value;
        _subsetMax = value;
        _numValues += count;
        _sumOfValues += (count * value.toEpochSecond(ZoneOffset.UTC));
      }

      @Override
      HistogramStats toHistogramStats(int uniqueEntityCount) {
        HistogramStats stats = new HistogramStatsImpl();
        stats.setSubsetMin(RequestBundle.formatDate(_subsetMin));
        stats.setSubsetMax(RequestBundle.formatDate(_subsetMax));
        stats.setSubsetMean(RequestBundle.formatDate(
            LocalDateTime.ofEpochSecond(_sumOfValues / _numValues, 0, ZoneOffset.UTC)));
        stats.setNumVarValues((int)_numValues); // FIXME: int cast ok here?
        stats.setNumDistinctEntityRecords(uniqueEntityCount);
        stats.setNumMissingCases(0); // FIXME: get from null tuple?
        return stats;
      }
    };
  }

  @Override
  protected DateBin getFirstBin() {
    LocalDateTime displayRangeMin = RequestBundle.parseDate(_variable.getDisplayRangeMin());
    LocalDateTime rangeMin = RequestBundle.parseDate(_variable.getRangeMin());
    LocalDateTime binStart = displayRangeMin.isBefore(rangeMin) ? displayRangeMin : rangeMin;
    binStart = binStart.truncatedTo(ChronoUnit.DAYS);
    // TODO: adjust binStart to be the beginning of the range for month, year
    return new DateBin(binStart, binStart.plus(1, _binUnits));
  }

  @Override
  protected DateBin getNextBin(DateBin currentBin) {
    return new DateBin(currentBin._end, currentBin._end.plus(1, _binUnits));
  }

  @Override
  protected List<HistogramBin> getExtraBins(DateBin currentBin) {
    LocalDateTime displayRangeMax = RequestBundle.parseDate(_variable.getDisplayRangeMax());
    LocalDateTime rangeMax = RequestBundle.parseDate(_variable.getRangeMax());
    LocalDateTime end = displayRangeMax.isAfter(rangeMax) ? displayRangeMax : rangeMax;
    List<HistogramBin> extraBins = new ArrayList<>();
    while (currentBin._end.isBefore(end)) {
      currentBin = new DateBin(currentBin._end, currentBin._end.plus(1, _binUnits));
      extraBins.add(currentBin.toHistogramBin());
    }
    return extraBins;
  }

}
