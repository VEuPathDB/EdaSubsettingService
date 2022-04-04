package org.veupathdb.service.eda.ss.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.sql.DataSource;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.gusdb.fgputil.FormatUtil;
import org.veupathdb.service.eda.generated.model.APIDateRangeFilter;
import org.veupathdb.service.eda.generated.model.APIDateSetFilter;
import org.veupathdb.service.eda.generated.model.APIFilter;
import org.veupathdb.service.eda.generated.model.APILongitudeRangeFilter;
import org.veupathdb.service.eda.generated.model.APIMultiFilter;
import org.veupathdb.service.eda.generated.model.APIMultiFilterSubFilter;
import org.veupathdb.service.eda.generated.model.APINumberRangeFilter;
import org.veupathdb.service.eda.generated.model.APINumberSetFilter;
import org.veupathdb.service.eda.generated.model.APIStringSetFilter;
import org.veupathdb.service.eda.generated.model.APITabularReportConfig;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.MetadataCache;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.variable.DateVariable;
import org.veupathdb.service.eda.ss.model.variable.LongitudeVariable;
import org.veupathdb.service.eda.ss.model.variable.NumberVariable;
import org.veupathdb.service.eda.ss.model.variable.StringVariable;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableDisplayType;
import org.veupathdb.service.eda.ss.model.filter.DateRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.DateSetFilter;
import org.veupathdb.service.eda.ss.model.filter.Filter;
import org.veupathdb.service.eda.ss.model.filter.LongitudeRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.MultiFilter;
import org.veupathdb.service.eda.ss.model.filter.MultiFilter.MultiFilterOperation;
import org.veupathdb.service.eda.ss.model.filter.MultiFilterSubFilter;
import org.veupathdb.service.eda.ss.model.filter.NumberRangeFilter;
import org.veupathdb.service.eda.ss.model.filter.NumberSetFilter;
import org.veupathdb.service.eda.ss.model.filter.StringSetFilter;

public class RequestBundle {

  static RequestBundle unpack(DataSource datasource, String studyId, String entityId, List<APIFilter> apiFilters, List<String> variableIds, APITabularReportConfig apiReportConfig) {
    String studIdStr = "Study ID " + studyId;
    if (!validateStudyId(datasource, studyId))
      throw new NotFoundException(studIdStr + " is not found.");

    Study study = MetadataCache.getStudy(studyId);
    Entity entity = study.getEntity(entityId).orElseThrow(() -> new NotFoundException("In " + studIdStr + " Entity ID not found: " + entityId));

    List<Variable> variables = getEntityVariables(entity, variableIds);

    List<Filter> filters = constructFiltersFromAPIFilters(study, apiFilters);

    TabularReportConfig reportConfig = new TabularReportConfig(entity, Optional.ofNullable(apiReportConfig));

    return new RequestBundle(study, entity, variables, filters, reportConfig);
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

      Filter newFilter;
      if (apiFilter instanceof APIDateRangeFilter) {
        newFilter = unpackDateRangeFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APIDateSetFilter) {
        newFilter = unpackDateSetFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APINumberRangeFilter) {
        newFilter = unpackNumberRangeFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APINumberSetFilter) {
        newFilter = unpackNumberSetFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APILongitudeRangeFilter) {
        newFilter = unpackLongitudeRangeFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APIStringSetFilter) {
        newFilter = unpackStringSetFilter(apiFilter, entity);
      }
      else if (apiFilter instanceof APIMultiFilter) {
        newFilter = unpackMultiFilter(apiFilter, entity);
      }
      else
        throw new InternalServerErrorException("Input filter not an expected subclass of Filter");

      subsetFilters.add(newFilter);
    }
    return subsetFilters;
  }

  private static DateRangeFilter unpackDateRangeFilter(APIFilter apiFilter, Entity entity) {
    APIDateRangeFilter f = (APIDateRangeFilter)apiFilter;
    DateVariable var = DateVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new DateRangeFilter(entity, var, FormatUtil.parseDateTime(standardizeLocalDateTime(f.getMin())), FormatUtil.parseDateTime(standardizeLocalDateTime(f.getMax())));
  }

  private static DateSetFilter unpackDateSetFilter(APIFilter apiFilter, Entity entity) {
    APIDateSetFilter f = (APIDateSetFilter)apiFilter;
    DateVariable var = DateVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    List<LocalDateTime> dateSet = new ArrayList<>();
    for (String dateStr : f.getDateSet()) dateSet.add(FormatUtil.parseDateTime(standardizeLocalDateTime(dateStr)));
    return new DateSetFilter(entity, var, dateSet);
  }

  private static NumberRangeFilter unpackNumberRangeFilter(APIFilter apiFilter, Entity entity) {
    APINumberRangeFilter f = (APINumberRangeFilter)apiFilter;
    NumberVariable<?> var = NumberVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new NumberRangeFilter(entity, var, f.getMin(), f.getMax());
  }

  private static NumberSetFilter unpackNumberSetFilter(APIFilter apiFilter, Entity entity) {
    APINumberSetFilter f = (APINumberSetFilter)apiFilter;
    NumberVariable<?> var = NumberVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new NumberSetFilter(entity, var, f.getNumberSet());
  }

  private static LongitudeRangeFilter unpackLongitudeRangeFilter(APIFilter apiFilter, Entity entity) {
    APILongitudeRangeFilter f = (APILongitudeRangeFilter)apiFilter;
    LongitudeVariable var = LongitudeVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new LongitudeRangeFilter(entity, var, f.getLeft(), f.getRight());
  }

  private static StringSetFilter unpackStringSetFilter(APIFilter apiFilter, Entity entity) {
    APIStringSetFilter f = (APIStringSetFilter)apiFilter;
    String varId = f.getVariableId();
    StringVariable stringVar = StringVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new StringSetFilter(entity, stringVar, f.getStringSet());
  }

  private static MultiFilter unpackMultiFilter(APIFilter apiFilter, Entity entity) {
    APIMultiFilter f = (APIMultiFilter)apiFilter;
    if (f.getSubFilters().isEmpty())
      throw new BadRequestException("Multifilter may not have an empty list of subFilters");

    // validate multifilter variable
    String mfVarId = f.getVariableId();
    Variable multiFilterVariable = entity.getVariable(mfVarId).orElseThrow(() ->
        new BadRequestException("Multifilter includes invalid multifilter variable ID: " + mfVarId));
    if (multiFilterVariable.getDisplayType() != VariableDisplayType.MULTIFILTER)
      throw new BadRequestException("Multifilter variable does not have display type 'multifilter': " + mfVarId);

    // validate subfilters
    List<MultiFilterSubFilter> subFilters = new ArrayList<>();
    for (APIMultiFilterSubFilter apiSubFilter : f.getSubFilters()) {
      String variableId = apiSubFilter.getVariableId();
      if (!entity.getMultiFilterMap().get(mfVarId).contains(variableId))
        throw new BadRequestException("Multifilter includes subfilter with invalid variable: " + variableId);
      Variable var = entity.getVariable(variableId).orElseThrow(() -> new BadRequestException("Multifilter includes invalid variable ID: " + variableId));
      subFilters.add(new MultiFilterSubFilter(var, apiSubFilter.getStringSet()));
    }

    return new MultiFilter(entity, subFilters, MultiFilterOperation.fromString(f.getOperation().getValue()));
  }

  // TODO: remove once the client is fixed to not send in trailing 'Z'
  public static String standardizeLocalDateTime(String dateTimeString) {
    return (dateTimeString == null || !dateTimeString.endsWith("Z"))
        ? dateTimeString
        : dateTimeString.substring(0, dateTimeString.length() - 1);
  }

  private final Study _study;
  private final List<Filter> _filters;
  private final Entity _targetEntity;
  private final List<Variable> _requestedVariables;
  private final TabularReportConfig _reportConfig;

  RequestBundle(Study study, Entity targetEntity, List<Variable> requestedVariables, List<Filter> filters, TabularReportConfig reportConfig) {
    _study = study;
    _targetEntity = targetEntity;
    _filters = filters;
    _requestedVariables = requestedVariables;
    _reportConfig = reportConfig;
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

  public TabularReportConfig getReportConfig() {
    return _reportConfig;
  }
}
