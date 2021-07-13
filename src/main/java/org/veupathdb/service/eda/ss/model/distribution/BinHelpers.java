package org.veupathdb.service.eda.ss.model.distribution;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramBinImpl;
import org.veupathdb.service.eda.ss.service.RequestBundle;

public class BinHelpers {

  public interface Bin<T> {
    boolean accept(T value, Long count);
    HistogramBin toHistogramBin();
  }

  public static class NumberBin implements Bin<Double> {

    public final double _start;
    public final double _end;

    private long _binCount = 0;

    public NumberBin(double start, double end) {
      _start = start;
      _end = end;
    }

    @Override
    public boolean accept(Double value, Long count) {
      if (value < _start) {
        // values should be arriving in ascending order; throw here to avoid infinite loop
        throw new RuntimeException("Distribution value " + value + " is less than this bin's min; this should not happen.");
      }
      if (value < _end) { // end is exclusive
        _binCount += count;
        return true;
      }
      return false;
    }

    @Override
    public HistogramBin toHistogramBin() {
      HistogramBin bin = new HistogramBinImpl();
      bin.setBinStart(String.valueOf(_start));
      bin.setBinEnd(String.valueOf(_end));
      bin.setBinLabel("[" + bin.getBinStart() + "," + bin.getBinEnd() + ")");
      bin.setValue(_binCount);
      return bin;
    }
  }

  public static class DateBin implements Bin<LocalDateTime> {

    public final LocalDateTime _start;
    public final LocalDateTime _end;

    private long _binCount = 0;

    public DateBin(LocalDateTime start, LocalDateTime end) {
      _start = start;
      _end = end;
    }

    @Override
    public boolean accept(LocalDateTime value, Long count) {
      if (value.isBefore(_start)) {
        // values should be arriving in ascending order; throw here to avoid infinite loop
        throw new RuntimeException("Distribution value " + RequestBundle.formatDate(value) +
            " is less than this bin's min; this should not happen.");
      }
      if (value.isBefore(_end)) { // end is exclusive
        _binCount += count;
        return true;
      }
      return false;
    }

    @Override
    public HistogramBin toHistogramBin() {
      HistogramBin bin = new HistogramBinImpl();
      bin.setBinStart(RequestBundle.formatDate(_start));
      bin.setBinEnd(RequestBundle.formatDate(_end));
      // TODO: fix label to be actual day, month, year (already accurate for week)
      bin.setBinLabel("[" + bin.getBinStart() + "," + bin.getBinEnd() + ")");
      bin.setValue(_binCount);
      return bin;
    }
  }
}