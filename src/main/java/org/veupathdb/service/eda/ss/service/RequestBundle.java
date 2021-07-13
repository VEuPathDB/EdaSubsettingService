package org.veupathdb.service.eda.ss.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import org.veupathdb.service.eda.generated.model.APIDateRangeFilter;
import org.veupathdb.service.eda.generated.model.APIDateSetFilter;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APILongitudeRangeFilter;
import org.veupathdb.service.eda.generated.model.APINumberRangeFilter;
import org.veupathdb.service.eda.generated.model.APINumberSetFilter;
import org.veupathdb.service.eda.generated.model.APIStringSetFilter;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.MetadataCache;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.Variable;
import org.veupathdb.service.eda.ss.model.filter.DateRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.DateSetFilter;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.model.filter.LongitudeRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.NumberRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.NumberSetFilter;
import org.veupathdb.service.eda.ss.model.filter.StringSetFilter;

public class RequestBundle {

  static RequestBundle unpack(DataSource datasource, String studyId, String entityId, List<APIFilter> apiFilters, List<String> variableIds) {
    String studIdStr = "Study ID " + studyId;
    if (!validateStudyId(datasource, studyId))
      throw new NotFoundException(studIdStr + " is not found.");

    Study study = MetadataCache.getStudy(studyId);
    Entity entity = study.getEntity(entityId).orElseThrow(() -> new NotFoundException("In " + studIdStr + " Entity ID not found: " + entityId));

    List<Variable> variables = getEntityVariables(entity, variableIds);

    List<Filter> filters = constructFiltersFromAPIFilters(study, apiFilters);

    return new RequestBundle(study, entity, variables, filters);
  }

  /*
   * return true if valid study id
   */
  private static boolean validateStudyId(DataSource datasource, String studyId) {
    return MetadataCache.getStudyOverviews().stream()
            .anyMatch(study -> study.getId().equals(studyId));
  }

  private static List<Variable> getEntityVariables(Entity entity, List<String> variableIds) {

    List<Variable> variables = new ArrayList<>();

    for (String varId : variableIds) {
      String errMsg = "Variable '" + varId + "' is not found for entity with ID: '" + entity.getId() + "'";
      variables.add(entity.getVariable(varId).orElseThrow(() -> new BadRequestException(errMsg)));
    }
    return variables;
  }
  /*
   * Given a study and a set of API filters, construct and return a set of filters, each being the appropriate
   * filter subclass
   */
  static List<Filter> constructFiltersFromAPIFilters(Study study, List<APIFilter> filters) {
    List<Filter> subsetFilters = new ArrayList<>();

    String errPrfx = "A filter references an unfound ";

    for (APIFilter apiFilter : filters) {

      // validate filter's entity id
      Supplier<BadRequestException> excep =
          () -> new BadRequestException(errPrfx + "entity ID: " + apiFilter.getEntityId());
      Entity entity = study.getEntity(apiFilter.getEntityId()).orElseThrow(excep);

      // validate filter's variable id
      String varId = apiFilter.getVariableId();
      entity.getVariable(varId).orElseThrow(() -> new BadRequestException("Variable '" + varId + "' is not found"));

      Filter newFilter;
      if (apiFilter instanceof APIDateRangeFilter) {
        APIDateRangeFilter f = (APIDateRangeFilter)apiFilter;
        newFilter = new DateRangeFilter(entity, varId,
            parseDate(f.getMin()), parseDate(f.getMax()));
      } else if (apiFilter instanceof APIDateSetFilter) {
        APIDateSetFilter f = (APIDateSetFilter)apiFilter;
        List<LocalDateTime> dateSet = new ArrayList<>();
        for (String dateStr : f.getDateSet()) dateSet.add(parseDate(dateStr));
        newFilter = new DateSetFilter(entity, varId, dateSet);
      } else if (apiFilter instanceof APINumberRangeFilter) {
        APINumberRangeFilter f = (APINumberRangeFilter)apiFilter;
        newFilter = new NumberRangeFilter(entity, varId, f.getMin(), f.getMax());
      } else if (apiFilter instanceof APINumberSetFilter) {
        APINumberSetFilter f = (APINumberSetFilter)apiFilter;
        newFilter = new NumberSetFilter(entity, varId, f.getNumberSet());
      } else if (apiFilter instanceof APILongitudeRangeFilter) {
        APILongitudeRangeFilter f = (APILongitudeRangeFilter)apiFilter;
        newFilter = new LongitudeRangeFilter(entity, varId, f.getLeft(), f.getRight());
      } else if (apiFilter instanceof APIStringSetFilter) {
        APIStringSetFilter f = (APIStringSetFilter)apiFilter;
        newFilter = new StringSetFilter(entity, varId, f.getStringSet());
      } else
        throw new InternalServerErrorException("Input filter not an expected subclass of Filter");

      subsetFilters.add(newFilter);
    }
    return subsetFilters;
  }

  public static LocalDateTime parseDate(String dateStr) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
      return LocalDateTime.parse(dateStr, formatter);
    }
    catch (DateTimeParseException e) {
      throw new BadRequestException("Can't parse date string: " + dateStr);
    }
  }

  public static String formatDate(LocalDateTime dateTime) {
    try {
      DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
      return formatter.format(dateTime);
    }
    catch (DateTimeParseException e) {
      throw new RuntimeException("Can't format date: " + dateTime);
    }
  }
  private final Study _study;
  private final List<Filter> _filters;
  private final Entity _targetEntity;
  private final List<Variable> _requestedVariables;

  RequestBundle(Study study, Entity targetEntity, List<Variable> requestedVariables, List<Filter> filters) {
    _study = study;
    _targetEntity = targetEntity;
    _filters = filters;
    _requestedVariables = requestedVariables;
  }

  public Study getStudy() {
    return _study;
  }

  public List<Filter> getFilters() {
    return _filters;
  }

  public Entity getTargetEntity() {
    return _targetEntity;
  }

  public List<Variable> getRequestedVariables() {
    return _requestedVariables;
  }
}
