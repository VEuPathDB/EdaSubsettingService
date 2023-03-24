package org.veupathdb.service.eda.ss.service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.SortDirection;
import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.eda.common.auth.StudyAccess;
import org.veupathdb.service.eda.common.client.DatasetAccessClient;
import org.veupathdb.service.eda.common.client.DatasetAccessClient.StudyDatasetInfo;
import org.veupathdb.service.eda.generated.model.*;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.distribution.BinSpecWithRange;
import org.veupathdb.service.eda.ss.model.distribution.BinUnits;
import org.veupathdb.service.eda.ss.model.distribution.DateDistributionConfig;
import org.veupathdb.service.eda.ss.model.distribution.NumberDistributionConfig;
import org.veupathdb.service.eda.ss.model.distribution.ValueSpec;
import org.veupathdb.service.eda.ss.model.tabular.TabularHeaderFormat;
import org.veupathdb.service.eda.ss.model.varcollection.DateVarCollection;
import org.veupathdb.service.eda.ss.model.varcollection.FloatingPointVarCollection;
import org.veupathdb.service.eda.ss.model.varcollection.IntegerVarCollection;
import org.veupathdb.service.eda.ss.model.varcollection.VarCollection;
import org.veupathdb.service.eda.ss.model.variable.DateVariable;
import org.veupathdb.service.eda.ss.model.variable.FloatingPointVariable;
import org.veupathdb.service.eda.ss.model.variable.IntegerVariable;
import org.veupathdb.service.eda.ss.model.variable.LongitudeVariable;
import org.veupathdb.service.eda.ss.model.variable.StringVariable;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableDataShape;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;

public class ApiConversionUtil {
  private static final Logger LOG = LogManager.getLogger(ApiConversionUtil.class);

  /** converts model study object to API study object */
  public static APIStudyDetail getApiStudyDetail(Study study) {
    APIEntity apiEntityTree = entityTreeToAPITree(study.getEntityTree());
    APIStudyDetail apiStudyDetail = new APIStudyDetailImpl();
    apiStudyDetail.setId(study.getStudyId());
    apiStudyDetail.setRootEntity(apiEntityTree);
    return apiStudyDetail;
  }

  /** converts model entity object to API entity object */
  public static APIEntity entityTreeToAPITree(TreeNode<Entity> root) {
    return root.mapStructure((entity, mappedChildren) -> {
      APIEntity apiEntity = new APIEntityImpl();
      apiEntity.setDescription(entity.getDescription());
      apiEntity.setDisplayName(entity.getDisplayName());
      apiEntity.setDisplayNamePlural(entity.getDisplayNamePlural());
      apiEntity.setId(entity.getId());
      apiEntity.setIdColumnName(entity.getPKColName());
      apiEntity.setIsManyToOneWithParent(entity.isManyToOneWithParent());
      apiEntity.setChildren(mappedChildren);
      apiEntity.setVariables(entity.getVariables().stream()
          .map(ApiConversionUtil::variableToAPIVariable)
          .collect(Collectors.toList()));
      apiEntity.setCollections(entity.getCollections().stream()
          .map(ApiConversionUtil::collectionToAPICollection)
          .collect(Collectors.toList()));
      return apiEntity;
    });
  }

  private static APICollection collectionToAPICollection(VarCollection<?,?> col) {
    APICollection collection = switch(col.getType()) {
      case DATE -> getDateCollection((DateVarCollection)col);
      case INTEGER -> getIntegerCollection((IntegerVarCollection)col);
      case NUMBER -> getFloatCollection((FloatingPointVarCollection)col);
    };
    collection.setId(col.getId());
    collection.setDisplayName(col.getDisplayName());
    collection.setDataShape(toApiShape(col.getDataShape()));
    collection.setImputeZero(col.getImputeZero());
    collection.setDistinctValuesCount(col.getDistinctValuesCount());
    collection.setVocabulary(col.getVocabulary());
    collection.setMemberVariableIds(col.getMemberVariableIds());
    return collection;
  }

  private static APIVariableDataShape toApiShape(VariableDataShape dataShape) {
    return APIVariableDataShape.valueOf(dataShape.name());
  }

  private static APICollection getDateCollection(DateVarCollection col) {
    APIDateCollection dateCol = new APIDateCollectionImpl();
    dateCol.setDistributionDefaults(getDateDistributionDefaults(col.getDistributionConfig()));
    return dateCol;
  }

  private static APICollection getIntegerCollection(IntegerVarCollection col) {
    APIIntegerCollection intCol = new APIIntegerCollectionImpl();
    intCol.setDistributionDefaults(getIntegerDistributionDefaults(col.getDistributionConfig()));
    intCol.setUnits(col.getUnits());
    return intCol;
  }

  private static APICollection getFloatCollection(FloatingPointVarCollection col) {
    APINumberCollection floatCol = new APINumberCollectionImpl();
    floatCol.setDistributionDefaults(getFloatDistributionDefaults(col.getDistributionConfig()));
    floatCol.setUnits(col.getUnits());
    floatCol.setPrecision(col.getPrecision());
    return floatCol;
  }

  /** converts model variable object to API variable object */
  private static APIVariable variableToAPIVariable(Variable var) {
    APIVariable apiVar = var.hasValues()
        ? getValuesVar((VariableWithValues<?>)var)
        : new APIVariablesCategoryImpl();
    // set props common to all variables
    apiVar.setId(var.getId());
    apiVar.setDisplayName(var.getDisplayName());
    apiVar.setDisplayType(APIVariableDisplayType.valueOf(var.getDisplayType().name()));
    apiVar.setProviderLabel(var.getProviderLabel());
    apiVar.setParentId(var.getParentId());
    apiVar.setDefinition(var.getDefinition());
    apiVar.setDisplayOrder(var.getDisplayOrder());
    apiVar.setHideFrom(var.getHideFrom() == null ? Collections.emptyList() : var.getHideFrom());
    return apiVar;
  }

  /** instantiates API values var and sets values-var-specific props */
  private static APIVariableWithValues getValuesVar(VariableWithValues<?> var) {
    APIVariableWithValues apiVar = switch(var.getType()) {
      case DATE -> getDateVar((DateVariable)var);
      case INTEGER -> getIntegerVar((IntegerVariable)var);
      case NUMBER -> getFloatVar((FloatingPointVariable)var);
      case LONGITUDE -> getLongitudeVar((LongitudeVariable)var);
      case STRING -> getStringVar((StringVariable)var);
    };
    // set props common to all variables with values
    apiVar.setDataShape(toApiShape(var.getDataShape()));
    apiVar.setVocabulary(var.getVocabulary());
    apiVar.setDistinctValuesCount(var.getDistinctValuesCount());
    apiVar.setIsFeatured(var.getIsFeatured());
    apiVar.setIsMergeKey(var.getIsMergeKey());
    apiVar.setIsMultiValued(var.getIsMultiValued());
    apiVar.setIsTemporal(var.getIsTemporal());
    apiVar.setImputeZero(var.getImputeZero());
    return apiVar;
  }

  /** instantiates API date var and sets date-specific props */
  private static APIDateVariable getDateVar(DateVariable var) {
    APIDateVariable apiVar = new APIDateVariableImpl();
    apiVar.setDistributionDefaults(getDateDistributionDefaults(var.getDistributionConfig()));
    return apiVar;
  }

  private static APIDateDistributionDefaults getDateDistributionDefaults(DateDistributionConfig distributionConfig) {
    APIDateDistributionDefaults defaults = new APIDateDistributionDefaultsImpl();
    defaults.setRangeMin(distributionConfig.rangeMin);
    defaults.setRangeMax(distributionConfig.rangeMax);
    defaults.setDisplayRangeMin(distributionConfig.displayRangeMin);
    defaults.setDisplayRangeMax(distributionConfig.displayRangeMax);
    defaults.setBinWidth(distributionConfig.binSize);
    defaults.setBinWidthOverride(null);
    defaults.setBinUnits(toApiBinUnits(distributionConfig.binUnits));
    return defaults;
  }

  private static APIIntegerVariable getIntegerVar(IntegerVariable var) {
    APIIntegerVariable apiVar = new APIIntegerVariableImpl();
    apiVar.setDistributionDefaults(getIntegerDistributionDefaults(var.getDistributionConfig()));
    apiVar.setUnits(var.getUnits());
    return apiVar;
  }

  private static APIIntegerDistributionDefaults getIntegerDistributionDefaults(NumberDistributionConfig<Long> distributionConfig) {
    APIIntegerDistributionDefaults defaults = new APIIntegerDistributionDefaultsImpl();
    defaults.setBinWidth(distributionConfig.getBinWidth());
    defaults.setBinWidthOverride(distributionConfig.getBinWidthOverride());
    defaults.setDisplayRangeMin(distributionConfig.getDisplayRangeMin());
    defaults.setDisplayRangeMax(distributionConfig.getDisplayRangeMax());
    defaults.setRangeMin(distributionConfig.getRangeMin());
    defaults.setRangeMax(distributionConfig.getRangeMax());
    return defaults;
  }

  private static APINumberVariable getFloatVar(FloatingPointVariable var) {
    APINumberVariable apiVar = new APINumberVariableImpl();
    apiVar.setDistributionDefaults(getFloatDistributionDefaults(var.getDistributionConfig()));
    apiVar.setUnits(var.getUnits());
    apiVar.setPrecision(var.getPrecision());
    return apiVar;
  }

  private static APINumberDistributionDefaults getFloatDistributionDefaults(NumberDistributionConfig<Double> distributionConfig) {
    APINumberDistributionDefaults defaults = new APINumberDistributionDefaultsImpl();
    defaults.setBinWidth(distributionConfig.getBinWidth());
    defaults.setBinWidthOverride(distributionConfig.getBinWidthOverride());
    defaults.setDisplayRangeMin(distributionConfig.getDisplayRangeMin());
    defaults.setDisplayRangeMax(distributionConfig.getDisplayRangeMax());
    defaults.setRangeMin(distributionConfig.getRangeMin());
    defaults.setRangeMax(distributionConfig.getRangeMax());
    return defaults;
  }

  private static APIStringVariable getStringVar(StringVariable var) {
    APIStringVariable apiStrVar = new APIStringVariableImpl();
    // currently no string-specific extra properties
    return apiStrVar;
  }

  private static APILongitudeVariable getLongitudeVar(LongitudeVariable var) {
    APILongitudeVariable apiVar = new APILongitudeVariableImpl();
    apiVar.setPrecision(var.getPrecision());
    return apiVar;
  }

  public static List<HistogramBin> toApiHistogramBins(List<org.gusdb.fgputil.distribution.HistogramBin> histogramData) {
    return histogramData.stream().map(modelBin -> {
      HistogramBin bin = new HistogramBinImpl();
      bin.setBinStart(modelBin.getBinStart());
      bin.setBinEnd(modelBin.getBinEnd());
      bin.setValue(modelBin.getValue());
      bin.setBinLabel(modelBin.getBinLabel());
      return bin;
    }).collect(Collectors.toList());
  }

  public static HistogramStats toApiHistogramStats(org.gusdb.fgputil.distribution.HistogramStats statistics) {
    HistogramStats stats = new HistogramStatsImpl();
    stats.setSubsetMin(statistics.getSubsetMin());
    stats.setSubsetMax(statistics.getSubsetMax());
    stats.setSubsetMean(statistics.getSubsetMean());
    stats.setSubsetSize(statistics.getSubsetSize());
    stats.setNumVarValues(statistics.getNumVarValues());
    stats.setNumDistinctValues(statistics.getNumDistinctValues());
    stats.setNumDistinctEntityRecords(statistics.getNumDistinctEntityRecords());
    stats.setNumMissingCases(statistics.getNumMissingCases());
    return stats;
  }

  public static Optional<BinSpecWithRange> toInternalBinSpecWithRange(org.veupathdb.service.eda.generated.model.BinSpecWithRange binSpec) {
    return Optional.ofNullable(binSpec)
        .map(b -> new BinSpecWithRange()
            .setDisplayRangeMin(b.getDisplayRangeMin())
            .setDisplayRangeMax(b.getDisplayRangeMax())
            .setBinWidth(b.getBinWidth())
            .setBinUnits(toInternalBinUnits(b.getBinUnits())));
  }

  private static BinUnits toInternalBinUnits(org.veupathdb.service.eda.generated.model.BinUnits binUnits) {
    return binUnits == null ? null : BinUnits.valueOf(binUnits.name());
  }

  private static org.veupathdb.service.eda.generated.model.BinUnits toApiBinUnits(BinUnits binUnits) {
    return binUnits == null ? null : org.veupathdb.service.eda.generated.model.BinUnits.valueOf(binUnits.name());
  }

  public static ValueSpec toInternalValueSpec(org.veupathdb.service.eda.generated.model.ValueSpec valueSpec) {
    return valueSpec == null ? null : ValueSpec.valueOf(valueSpec.name());
  }

  public static List<org.veupathdb.service.eda.ss.model.tabular.SortSpecEntry> toInternalSorting(List<SortSpecEntry> sorting) {
    return sorting == null ? null : sorting.stream()
        .map(e -> new org.veupathdb.service.eda.ss.model.tabular.SortSpecEntry()
            .setDirection(SortDirection.valueOf(e.getDirection().name()))
            .setKey(e.getKey()))
        .collect(Collectors.toList());
  }

  public static TabularHeaderFormat toInternalTabularHeaderFormat(org.veupathdb.service.eda.generated.model.TabularHeaderFormat headerFormat) {
    return headerFormat == null ? null : TabularHeaderFormat.valueOf(headerFormat.name());
  }

  public static org.veupathdb.service.eda.ss.model.tabular.DataSourceType toInternalDataSourceType(DataSourceType dataSourceType) {
    return org.veupathdb.service.eda.ss.model.tabular.DataSourceType.fromValue(dataSourceType.getValue());
  }

  public static List<APIStudyOverview> toApiStudyOverviews(
      Map<String, StudyDatasetInfo> datasetInfoMap,
      Map<String, StudyOverview> overviewMap) {
    return datasetInfoMap.keySet().stream()
        .filter(studyId -> {
          final boolean studyInOverviews = overviewMap.containsKey(studyId);
          if (!studyInOverviews) {
            LOG.warn("Found study " + studyId + " in visible studies but not it's not being returned from canoncial list of studies.");
          }
          return studyInOverviews;
        })
        .map(studyId -> {
          StudyDatasetInfo dataset = datasetInfoMap.get(studyId);
          StudyOverview overview = overviewMap.get(studyId);
          APIStudyOverview study = new APIStudyOverviewImpl();
          study.setId(studyId);
          study.setSourceType(switch (overview.getStudySourceType()) {
            case CURATED -> StudySourceType.CURATED;
            case USER_SUBMITTED -> StudySourceType.USERSUBMITTED;
          });
          study.setDatasetId(dataset.getDatasetId());
          study.setSha1hash(dataset.getSha1Hash());
          study.setDisplayName(dataset.getDisplayName());
          study.setShortDisplayName(dataset.getShortDisplayName());
          study.setDescription(dataset.getDescription());
          study.setLastModified(overview.getLastModified());
          return study;
        }).collect(Collectors.toList());
  }

}
