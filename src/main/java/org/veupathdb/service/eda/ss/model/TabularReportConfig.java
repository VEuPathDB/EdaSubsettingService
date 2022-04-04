package org.veupathdb.service.eda.ss.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jakarta.ws.rs.BadRequestException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.generated.model.APITabularReportConfig;
import org.veupathdb.service.eda.generated.model.SortSpecEntry;
import org.veupathdb.service.eda.generated.model.TabularHeaderFormat;

public class TabularReportConfig {
  private static final Logger LOG = LogManager.getLogger(TabularReportConfig.class);

  private List<SortSpecEntry> _sorting = new ArrayList<>();
  private Optional<Long> _numRows = Optional.empty();
  private Long _offset = 0L;
  private TabularHeaderFormat _headerFormat = TabularHeaderFormat.STANDARD;
  private boolean _trimTimeFromDateVars = false;

  public TabularReportConfig(Entity entity, Optional<APITabularReportConfig> configOpt) {
    if (configOpt.isEmpty()) {
      // use defaults for config
      return;
    }
    APITabularReportConfig apiConfig = configOpt.get();

    // assign submitted paging if present
    if (apiConfig.getPaging() != null) {
      LOG.info("Num rows type: {}", apiConfig.getPaging().getNumRows().getClass());
      Long numRows = apiConfig.getPaging().getNumRows();
      if (numRows != null) {
        if (numRows <= 0)
          throw new BadRequestException("In paging config, numRows must a positive integer.");
        _numRows = Optional.of(numRows);
      }
      Long offset = apiConfig.getPaging().getOffset();
      if (offset != null) {
        if (offset < 0)
          throw new BadRequestException("In paging config, offset must a non-negative integer.");
        _offset = offset;
      }
    }

    // assign submitted sorting if present
    List<SortSpecEntry> sorting = apiConfig.getSorting();
    if (sorting != null && !sorting.isEmpty()) {
      for (SortSpecEntry entry : sorting) {
        entity.getVariableOrThrow(entry.getKey());
      }
      _sorting = sorting;
    }

    // assign header format if present
    if (apiConfig.getHeaderFormat() != null) {
      _headerFormat = apiConfig.getHeaderFormat();
    }

    // assign date trimming flag if present
    if (apiConfig.getTrimTimeFromDateVars() != null) {
      _trimTimeFromDateVars = apiConfig.getTrimTimeFromDateVars();
    }
  }

  /**
   * Whether this configuration contains paging or sorting (paging always requires sorting)
   *
   * @return true if paging or sorting config is not the default, else false
   */
  public boolean requiresSorting() {
    return !_sorting.isEmpty() || !_numRows.isEmpty() || _offset != 0L;
  }

  public List<SortSpecEntry> getSorting() {
    return _sorting;
  }

  public Optional<Long> getNumRows() {
    return _numRows;
  }

  public Long getOffset() {
    return _offset;
  }

  public TabularHeaderFormat getHeaderFormat() {
    return _headerFormat;
  }

  public boolean getTrimTimeFromDateVars() {
    return _trimTimeFromDateVars;
  }
}
