package org.veupathdb.service.eda.ss.model.filter;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.ws.rs.BadRequestException;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.RdbmsColumnNames;
import org.veupathdb.service.eda.ss.model.variable.StringVariable;

import static org.gusdb.fgputil.FormatUtil.NL;

public class StringSetFilter extends SingleValueFilter<StringVariable> {

  private final List<String> _stringSet;
  
  public StringSetFilter(Entity entity, StringVariable variable, List<String> stringSet) {
    super(entity, variable);
    _stringSet = stringSet;
  }

  @Override
  public String getFilteringAndClausesSql() {
    return "  AND " + RdbmsColumnNames.STRING_VALUE_COL_NAME + " IN (" + createSqlInExpression() + ")" + NL;
  }

  private String createSqlInExpression() {

    // validate values against this var's vocabulary
    /* FIXME: add back at a less risky time, after validating DB data (ClinEpi Phase 2?)
    List<String> vocab = Optional.ofNullable(_variable.getVocabulary())
        .orElseThrow(() -> new RuntimeException("Var " + _variable.getId() + " has null vocabulary."));
    for (String value : _stringSet) {
      if (!vocab.contains(value)) {
        throw new BadRequestException("Value '" + value +
            "' is not in the vocabulary of variable '" +
            _variable.getId() + "' [ " + String.join(", ", vocab) + " ].");
      }
    }*/

    // process the validated list
    return _stringSet.stream()
        // replace single quotes with two single quotes (sql escape)
        .map(s -> s.replaceAll("'", "''"))
        // wrap in quotes
        .map(s -> "'" + s + "'")
        // join with commas
        .collect(Collectors.joining(", "));
  }

}
