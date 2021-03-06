package org.veupathdb.service.eda.ss.model.filter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.veupathdb.service.eda.ss.model.Entity;

import static org.gusdb.fgputil.FormatUtil.NL;
import static org.veupathdb.service.eda.ss.model.RdbmsColumnNames.DATE_VALUE_COL_NAME;

public class DateSetFilter extends Filter {

  private List<LocalDateTime> dateSet;

   
  public DateSetFilter(Entity entity, String variableId, List<LocalDateTime> dateSet) {
    super(entity, variableId);
    this.dateSet = dateSet;
  }

  @Override
  public String getAndClausesSql() {
    List<String> dateStrings = new ArrayList<>();
    for (LocalDateTime date : dateSet) dateStrings.add(DateRangeFilter.dbDateTimeIsoValue(date));
    return "  AND " + DATE_VALUE_COL_NAME + " IN (" + String.join(", ", dateStrings) + ")" + NL;
  }

}
