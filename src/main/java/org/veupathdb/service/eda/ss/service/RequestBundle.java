package org.veupathdb.service.eda.ss.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.NotFoundException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import org.veupathdb.service.eda.generated.model.SortSpecEntry;
import org.veupathdb.service.eda.ss.Utils;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.tabular.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.variable.*;
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

  private static final Logger LOG = LogManager.getLogger(RequestBundle.class);

  static RequestBundle unpack(String dataSchema, Study study, String entityId, List<APIFilter> apiFilters, List<String> variableIds, APITabularReportConfig apiReportConfig) {

    Entity entity = study.getEntity(entityId).orElseThrow(() -> new NotFoundException("In " + study.getStudyId() + " Entity ID not found: " + entityId));

    List<Variable> variables = getEntityVariables(entity, variableIds);

    List<Filter> filters = constructFiltersFromAPIFilters(study, apiFilters, dataSchema);

    TabularReportConfig reportConfig = getTabularReportConfig(entity, Optional.ofNullable(apiReportConfig));

    return new RequestBundle(study, entity, variables, filters, reportConfig);
  }

  private static TabularReportConfig getTabularReportConfig(Entity entity, Optional<APITabularReportConfig> configOpt) {
    TabularReportConfig config = new TabularReportConfig();

    if (configOpt.isEmpty()) {
      // use defaults for config
      return config;
    }

    APITabularReportConfig apiConfig = configOpt.get();

    // assign submitted paging if present
    if (apiConfig.getPaging() != null) {
      LOG.info("Num rows type: {}", apiConfig.getPaging().getNumRows().getClass());
      Long numRows = apiConfig.getPaging().getNumRows();
      if (numRows != null) {
        if (numRows <= 0)
          throw new BadRequestException("In paging config, numRows must a positive integer.");
        config.setNumRows(Optional.of(numRows));
      }
      Long offset = apiConfig.getPaging().getOffset();
      if (offset != null) {
        if (offset < 0)
          throw new BadRequestException("In paging config, offset must a non-negative integer.");
        config.setOffset(offset);
      }
    }

    // assign submitted sorting if present
    List<SortSpecEntry> sorting = apiConfig.getSorting();
    if (sorting != null && !sorting.isEmpty()) {
      for (SortSpecEntry entry : sorting) {
        entity.getVariableOrThrow(entry.getKey());
      }
      config.setSorting(ApiConversionUtil.toInternalSorting(sorting));
    }

    // assign header format if present
    if (apiConfig.getHeaderFormat() != null) {
      config.setHeaderFormat(ApiConversionUtil.toInternalTabularHeaderFormat(apiConfig.getHeaderFormat()));
    }

    // assign date trimming flag if present
    if (apiConfig.getTrimTimeFromDateVars() != null) {
      config.setTrimTimeFromDateVars(apiConfig.getTrimTimeFromDateVars());
    }

    if (apiConfig.getDataSource() != null) {
      config.setDataSourceType(ApiConversionUtil.toInternalDataSourceType(apiConfig.getDataSource()));
    }
    return config;
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
  static List<Filter> constructFiltersFromAPIFilters(Study study, List<APIFilter> filters, String appDbSchema) {
    List<Filter> subsetFilters = new ArrayList<>();

    // FIXME: need this until we turn on schema-level checking to enforce requiredness
    if (filters == null) return subsetFilters;

    String errPrfx = "A filter references an unfound ";

    for (APIFilter apiFilter : filters) {

      // validate filter's entity id
      Supplier<BadRequestException> excep =
          () -> new BadRequestException(errPrfx + "entity ID: " + apiFilter.getEntityId());
      Entity entity = study.getEntity(apiFilter.getEntityId()).orElseThrow(excep);

      Filter newFilter;
      if (apiFilter instanceof APIDateRangeFilter) {
        newFilter = unpackDateRangeFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APIDateSetFilter) {
        newFilter = unpackDateSetFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APINumberRangeFilter) {
        newFilter = unpackNumberRangeFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APINumberSetFilter) {
        newFilter = unpackNumberSetFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APILongitudeRangeFilter) {
        newFilter = unpackLongitudeRangeFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APIStringSetFilter) {
        newFilter = unpackStringSetFilter(apiFilter, entity, appDbSchema);
      }
      else if (apiFilter instanceof APIMultiFilter) {
        newFilter = unpackMultiFilter(apiFilter, entity, appDbSchema);
      }
      else
        throw new InternalServerErrorException("Input filter not an expected subclass of Filter");

      subsetFilters.add(newFilter);
    }
    return subsetFilters;
  }

  private static DateRangeFilter unpackDateRangeFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APIDateRangeFilter f = (APIDateRangeFilter)apiFilter;
    if (f.getMin() == null) throw new BadRequestException("Date range filter: min is a required property");
    if (f.getMax() == null) throw new BadRequestException("Date range filter: max is a required property");
    DateVariable var = DateVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new DateRangeFilter(appDbSchema, entity, var,
        FormatUtil.parseDateTime(Utils.standardizeLocalDateTime(f.getMin())),
        FormatUtil.parseDateTime(Utils.standardizeLocalDateTime(f.getMax())));
  }

  private static DateSetFilter unpackDateSetFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APIDateSetFilter f = (APIDateSetFilter)apiFilter;
    if (f.getDateSet() == null || f.getDateSet().isEmpty())
      throw new BadRequestException(("Date set filter: >0 dates must be specified"));
    DateVariable var = DateVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    List<LocalDateTime> dateSet = new ArrayList<>();
    for (String dateStr : f.getDateSet()) {
      dateSet.add(FormatUtil.parseDateTime(Utils.standardizeLocalDateTime(dateStr)));
    }
    return new DateSetFilter(appDbSchema, entity, var, dateSet);
  }

  private static NumberRangeFilter<?> unpackNumberRangeFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APINumberRangeFilter f = (APINumberRangeFilter)apiFilter;
    if (f.getMin() == null) throw new BadRequestException("Number range filter: min is a required property");
    if (f.getMax() == null) throw new BadRequestException("Number range filter: max is a required property");

    Variable var = entity.getVariableOrThrow(f.getVariableId());

    // need to check for each number variable type
    Optional<NumberRangeFilter<Long>> intFilter = IntegerVariable.assertType(var)
        .map(intVar -> new NumberRangeFilter<>(appDbSchema, entity, intVar, f.getMin(), f.getMax()));
    if (intFilter.isPresent()) return intFilter.get();

    // not integer var; try floating point or else throw
    return FloatingPointVariable.assertType(var)
        .map(floatVar -> new NumberRangeFilter<>(appDbSchema, entity, floatVar, f.getMin(), f.getMax()))
        .orElseThrow(() -> new BadRequestException("Variable " + var.getId() +
            " of entity " + var.getEntityId() + " is not a number or integer variable."));
  }

  private static NumberSetFilter<?> unpackNumberSetFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APINumberSetFilter f = (APINumberSetFilter)apiFilter;
    if (f.getNumberSet() == null || f.getNumberSet().isEmpty())
      throw new BadRequestException(("Number set filter: >0 numbers must be specified"));

    Variable var = entity.getVariableOrThrow(f.getVariableId());

    // need to check for each number variable type
    Optional<NumberSetFilter<Long>> intFilter = IntegerVariable.assertType(var)
        .map(intVar -> new NumberSetFilter<>(appDbSchema, entity, intVar, f.getNumberSet()));
    if (intFilter.isPresent()) return intFilter.get();

    // not integer var; try floating point or else throw
    return FloatingPointVariable.assertType(var)
        .map(floatVar -> new NumberSetFilter<>(appDbSchema, entity, floatVar, f.getNumberSet()))
        .orElseThrow(() -> new BadRequestException("Variable " + var.getId() +
            " of entity " + var.getEntityId() + " is not a number or integer variable."));
  }

  private static LongitudeRangeFilter unpackLongitudeRangeFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APILongitudeRangeFilter f = (APILongitudeRangeFilter)apiFilter;
    if (f.getLeft() == null) throw new BadRequestException("Longitude range filter: left is a required property");
    if (f.getRight() == null) throw new BadRequestException("Longitude range filter: right is a required property");
    LongitudeVariable var = LongitudeVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new LongitudeRangeFilter(appDbSchema, entity, var, f.getLeft(), f.getRight());
  }

  private static StringSetFilter unpackStringSetFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
    APIStringSetFilter f = (APIStringSetFilter)apiFilter;
    if (f.getStringSet() == null || f.getStringSet().isEmpty())
      throw new BadRequestException(("String set filter: >0 strings must be specified"));
    StringVariable stringVar = StringVariable.assertType(entity.getVariableOrThrow(f.getVariableId()));
    return new StringSetFilter(appDbSchema, entity, stringVar, f.getStringSet());
  }

  private static MultiFilter unpackMultiFilter(APIFilter apiFilter, Entity entity, String appDbSchema) {
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
      StringVariable subFilterVar = StringVariable.assertType(var);
      subFilters.add(new MultiFilterSubFilter(subFilterVar, apiSubFilter.getStringSet()));
    }

    return new MultiFilter(appDbSchema, entity, subFilters, MultiFilterOperation.fromString(f.getOperation().getValue()));
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
