package org.veupathdb.service.eda.ss.service;

import java.util.*;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.Tuples;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.functional.TreeNode;
import org.veupathdb.service.eda.generated.model.APIDateVariable;
import org.veupathdb.service.eda.generated.model.APIDateVariableImpl;
import org.veupathdb.service.eda.generated.model.APIEntity;
import org.veupathdb.service.eda.generated.model.APIEntityImpl;
import org.veupathdb.service.eda.generated.model.APILongitudeVariable;
import org.veupathdb.service.eda.generated.model.APILongitudeVariableImpl;
import org.veupathdb.service.eda.generated.model.APINumberVariable;
import org.veupathdb.service.eda.generated.model.APINumberVariableImpl;
import org.veupathdb.service.eda.generated.model.APIStringVariable;
import org.veupathdb.service.eda.generated.model.APIStringVariableImpl;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.APIStudyDetailImpl;
import org.veupathdb.service.eda.generated.model.APIVariable;
import org.veupathdb.service.eda.generated.model.APIVariableDataShape;
import org.veupathdb.service.eda.generated.model.APIVariableDisplayType;
import org.veupathdb.service.eda.generated.model.APIVariablesCategory;
import org.veupathdb.service.eda.generated.model.APIVariablesCategoryImpl;
import org.veupathdb.service.eda.generated.model.BinSpec;
import org.veupathdb.service.eda.generated.model.BinSpecImpl;
import org.veupathdb.service.eda.generated.model.BinUnits;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponse;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponse;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostResponseStream;
import org.veupathdb.service.eda.generated.model.HistogramBin;
import org.veupathdb.service.eda.generated.model.HistogramStats;
import org.veupathdb.service.eda.generated.model.StudiesGetResponseImpl;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponse;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponse;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponseImpl;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.MetadataCache;
import org.veupathdb.service.eda.ss.model.VariableWithValues;
import org.veupathdb.service.eda.ss.model.distribution.AbstractDistribution;
import org.veupathdb.service.eda.ss.model.distribution.DateBinDistribution;
import org.veupathdb.service.eda.ss.model.distribution.DiscreteDistribution;
import org.veupathdb.service.eda.ss.model.distribution.DistributionResult;
import org.veupathdb.service.eda.ss.model.distribution.NumberBinDistribution;
import org.veupathdb.service.eda.ss.model.DateVariable;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.NumberVariable;
import org.veupathdb.service.eda.ss.model.StringVariable;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudySubsettingUtils;
import org.veupathdb.service.eda.ss.model.Variable;
import org.veupathdb.service.eda.ss.model.Variable.VariableType;
import org.veupathdb.service.eda.ss.model.filter.Filter;

public class Studies implements org.veupathdb.service.eda.generated.resources.Studies {

  @Override
  public GetStudiesClearMetadataCacheResponse getStudiesClearMetadataCache() {
    MetadataCache.clear();
    return GetStudiesClearMetadataCacheResponse.respond202();
  }

  @Override
  public GetStudiesResponse getStudies() {
    var out = new StudiesGetResponseImpl();
    out.setStudies(MetadataCache.getStudyOverviews());
    return GetStudiesResponse.respond200WithApplicationJson(out);
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    Study study = MetadataCache.getStudy(studyId);
    APIStudyDetail apiStudyDetail = getApiStudyDetail(study);
    StudyIdGetResponse response = new StudyIdGetResponseImpl();
    response.setStudy(apiStudyDetail);
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    APIStudyDetail apiStudyDetail = getApiStudyDetail(MetadataCache.getStudy(studyId));
    APIEntity entity = findEntityById(apiStudyDetail.getRootEntity(), entityId).orElseThrow(NotFoundException::new);
    EntityIdGetResponse response = new EntityIdGetResponseImpl();
    // copy properties of found entity, skipping children
    response.setId(entity.getId());
    response.setDescription(entity.getDescription());
    response.setDisplayName(entity.getDisplayName());
    response.setDisplayNamePlural(entity.getDisplayNamePlural());
    response.setVariables(entity.getVariables());
    for (Map.Entry<String,Object> prop : entity.getAdditionalProperties().entrySet()) {
      response.setAdditionalProperties(prop.getKey(), prop.getValue());
    }
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

  private Optional<APIEntity> findEntityById(APIEntity entity, String entityId) {
    if (entity.getId().equals(entityId)) {
      return Optional.of(entity);
    }
    for (APIEntity child : entity.getChildren()){
      Optional<APIEntity> foundEntity = findEntityById(child, entityId);
      if (foundEntity.isPresent()) return foundEntity;
    }
    return Optional.empty();
  }

  public static APIStudyDetail getApiStudyDetail(Study study) {
    APIEntity apiEntityTree = entityTreeToAPITree(study.getEntityTree());
    APIStudyDetail apiStudyDetail = new APIStudyDetailImpl();
    apiStudyDetail.setId(study.getStudyId());
    apiStudyDetail.setDatasetId(study.getDatasetId());
    apiStudyDetail.setRootEntity(apiEntityTree);
    return apiStudyDetail;
  }
  
  public static APIEntity entityTreeToAPITree(TreeNode<Entity> root) {
    return root.mapStructure((entity, mappedChildren) -> {
      APIEntity apiEntity = new APIEntityImpl();
      apiEntity.setDescription(entity.getDescription());
      apiEntity.setDisplayName(entity.getDisplayName());
      apiEntity.setDisplayNamePlural(entity.getDisplayNamePlural());
      apiEntity.setId(entity.getId());
      apiEntity.setIdColumnName(entity.getPKColName());
      apiEntity.setChildren(mappedChildren);
      apiEntity.setVariables(entity.getVariables().stream()
          .map(var -> variableToAPIVariable(var))
          .collect(Collectors.toList()));
      return apiEntity;
    });
  }
  
  private static APIVariable variableToAPIVariable(Variable var) {
    if (!var.getHasValues()) {
      APIVariablesCategory apiVar = new APIVariablesCategoryImpl();
      apiVar.setId(var.getId());
      apiVar.setDisplayName(var.getDisplayName());
      apiVar.setProviderLabel(var.getProviderLabel());
      apiVar.setParentId(var.getParentId());
      apiVar.setDefinition(var.getDefinition());
      apiVar.setDisplayOrder(var.getDisplayOrder());
      return apiVar;
    }
    if (var.getType() == VariableType.DATE) {
      DateVariable dateVar = (DateVariable)var;
      APIDateVariable apiVar = new APIDateVariableImpl();
      setApiVarProps(apiVar, var);
      apiVar.setBinWidth(dateVar.getBinUnits().getValue());
      apiVar.setBinWidthOverride(Optional.ofNullable(dateVar.getBinUnitsOverride()).map(BinUnits::getValue).orElse(null));
      apiVar.setDisplayRangeMin(dateVar.getDisplayRangeMin());
      apiVar.setDisplayRangeMax(dateVar.getDisplayRangeMax());
      apiVar.setRangeMin(dateVar.getRangeMin());
      apiVar.setRangeMax(dateVar.getRangeMax());
      apiVar.setVocabulary(dateVar.getVocabulary());
      apiVar.setDistinctValuesCount(dateVar.getDistinctValuesCount());
      apiVar.setIsFeatured(dateVar.getIsFeatured());
      apiVar.setIsMergeKey(dateVar.getIsMergeKey());
      apiVar.setIsMultiValued(dateVar.getIsMultiValued());
      apiVar.setIsTemporal(dateVar.getIsTemporal());
      return apiVar;
    }
    else if (var.getType() == VariableType.NUMBER) {
      NumberVariable numVar = (NumberVariable)var;
      APINumberVariable apiVar = new APINumberVariableImpl();
      setApiVarProps(apiVar, var);
      apiVar.setUnits(numVar.getUnits());
      apiVar.setPrecision(numVar.getPrecision());
      apiVar.setBinWidth(numVar.getBinWidth());
      apiVar.setBinWidthOverride(numVar.getBinWidthOverride());
      apiVar.setDisplayRangeMin(numVar.getDisplayRangeMin());
      apiVar.setDisplayRangeMax(numVar.getDisplayRangeMax());
      apiVar.setRangeMin(numVar.getRangeMin());
      apiVar.setRangeMax(numVar.getRangeMax());
      apiVar.setDistinctValuesCount(numVar.getDistinctValuesCount());
      apiVar.setIsFeatured(numVar.getIsFeatured());
      apiVar.setIsMergeKey(numVar.getIsMergeKey());
      apiVar.setIsMultiValued(numVar.getIsMultiValued());
      apiVar.setIsTemporal(numVar.getIsTemporal());
      apiVar.setVocabulary(numVar.getVocabulary());
      return apiVar;
    }
    else if (var.getType() == VariableType.STRING) {
      StringVariable strVar = (StringVariable)var;	
      APIStringVariable apiVar = new APIStringVariableImpl();
      setApiVarProps(apiVar, var);
      apiVar.setDistinctValuesCount(strVar.getDistinctValuesCount());
      apiVar.setIsFeatured(strVar.getIsFeatured());
      apiVar.setIsMergeKey(strVar.getIsMergeKey());
      apiVar.setIsMultiValued(strVar.getIsMultiValued());
      apiVar.setIsTemporal(strVar.getIsTemporal());
      apiVar.setVocabulary(strVar.getVocabulary());
      return apiVar;
    }
    else if (var.getType() == VariableType.LONGITUDE) {
      APILongitudeVariable apiVar = new APILongitudeVariableImpl();
      setApiVarProps(apiVar, var);
      return apiVar;
    }
    else {
      throw new RuntimeException("Invalid variable type " + var.getType());
    }
  }
  
  private static void setApiVarProps(APIVariable apiVar, Variable var) {
    apiVar.setId(var.getId());
    apiVar.setDataShape(APIVariableDataShape.valueOf(var.getDataShape().toString()));
    apiVar.setDisplayType(APIVariableDisplayType.valueOf(var.getDisplayType().toString()));
    apiVar.setDisplayName(var.getDisplayName());
    apiVar.setProviderLabel(var.getProviderLabel());
    apiVar.setParentId(var.getParentId());
    apiVar.setDefinition(var.getDefinition());
    apiVar.setDisplayOrder(var.getDisplayOrder());
  }

  @Override
  public PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse 
  postStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableId(
      String studyId, String entityId, String variableId, VariableDistributionPostRequest request) {

    // unpack data from API input to model objects
    DataSource ds = Resources.getApplicationDataSource();
    RequestBundle req = RequestBundle.unpack(ds, studyId, entityId, request.getFilters(), ListBuilder.asList(variableId));
    VariableWithValues var = getRequestedVariable(req);

    DistributionResult result = processDistributionRequest(ds, req.getStudy(),
        req.getTargetEntity(), var, req.getFilters(), request.getValueSpec(),
        Optional.ofNullable(request.getBinSpec()));

    VariableDistributionPostResponse response = new VariableDistributionPostResponseImpl();
    response.setHistogram(result.getHistogramData());
    response.setStatistics(result.getStatistics());

    return PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse.
        respond200WithApplicationJson(response);
  }

  static DistributionResult processDistributionRequest(DataSource ds, Study study, Entity targetEntity,
                                                       VariableWithValues var, List<Filter> filters, VariableDistributionPostRequest.ValueSpecType valueSpec, Optional<BinSpec> incomingBinSpec) {
    // inspect requested variable and select appropriate distribution
    AbstractDistribution<?> distribution;
    if (var.getDataShape() == Variable.VariableDataShape.CONTINUOUS) {
      BinSpec binSpec = validateVarAndBinSpec(var, incomingBinSpec);
      distribution = switch(var.getType()) {
        case NUMBER -> new NumberBinDistribution(ds, study, targetEntity,
            (NumberVariable)var, filters, valueSpec, binSpec);
        case DATE -> new DateBinDistribution(ds, study, targetEntity,
            (DateVariable)var, filters, valueSpec, binSpec);
        default -> throw new BadRequestException(
            "Requested variable '" + var.getId() + "' must be a date or number, but is " + var.getType());
      };
    }
    else {
      if (incomingBinSpec.isPresent()) {
        throw new BadRequestException("Bin spec is allowed/required only for continuous variables.");
      }
      distribution = new DiscreteDistribution(ds, study, targetEntity, var, filters, valueSpec);
    }

    return distribution.generateDistribution();
  }

  private VariableWithValues getRequestedVariable(RequestBundle req) {
    if (req.getRequestedVariables().isEmpty()) {
      throw new RuntimeException("No requested variables (empty URL segment?)");
    }
    Variable var = req.getRequestedVariables().get(0);
    if (!(var instanceof VariableWithValues)) {
      throw new BadRequestException("Distribution endpoint can only be called with a variable that has values.");
    }
    return (VariableWithValues)var;
  }

  private static BinSpec validateVarAndBinSpec(Variable var, Optional<BinSpec> submittedBinSpec) {
    // bin spec is needed; type determined by var type
    switch(var.getType()) {
      case STRING:
      case LONGITUDE:
        throw new BadRequestException("Among continuous variables, " +
            "distribution endpoint supports only date and number types.");
      case DATE:
        DateVariable dateVar = (DateVariable)var;
        if (submittedBinSpec.isEmpty()) {
          // use defaults
          BinSpec binSpec = new BinSpecImpl();
          binSpec.setDisplayRangeMin(dateVar.getDisplayRangeMin());
          binSpec.setDisplayRangeMax(dateVar.getDisplayRangeMax());
          binSpec.setBinUnits(dateVar.getDefaultBinUnits());
          return binSpec;
        }
        else {
          BinSpec binSpec = submittedBinSpec.get();
          if (binSpec.getBinUnits() == null || binSpec.getBinWidth() != null) {
            throw new BadRequestException("For date variables, only binUnits should be submitted (not binWidth).");
          }
          return binSpec;
        }
      case NUMBER:
        NumberVariable numberVar = (NumberVariable)var;
        if (submittedBinSpec.isEmpty()) {
          // use defaults
          BinSpec binSpec = new BinSpecImpl();
          binSpec.setDisplayRangeMin(numberVar.getDisplayRangeMin());
          binSpec.setDisplayRangeMax(numberVar.getDisplayRangeMax());
          binSpec.setBinWidth(numberVar.getDefaultBinWidth());
          return binSpec;
        }
        else {
          BinSpec binSpec = submittedBinSpec.get();
          if (binSpec.getBinUnits() != null || binSpec.getBinWidth() == null) {
            throw new BadRequestException("For number variables, only binWidth should be submitted (not binUnits).");
          }
          return binSpec;
        }
      default:
        throw new RuntimeException("Must add case statement here for type: " + var.getType());
    }
  }

  @Override
  public PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse postStudiesEntitiesTabularByStudyIdAndEntityId(String studyId,
      String entityId, EntityTabularPostRequest requestBody) {
    
    DataSource datasource = Resources.getApplicationDataSource();
    
    RequestBundle request = RequestBundle.unpack(datasource, studyId, entityId, requestBody.getFilters(), requestBody.getOutputVariableIds());
    
    EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream
        (outStream -> StudySubsettingUtils.produceTabularSubset(datasource, request.getStudy(),
            request.getTargetEntity(), request.getRequestedVariables(), request.getFilters(), outStream));

    return PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse
        .respond200WithTextPlain(streamer);
  }

  @Override
  public PostStudiesEntitiesCountByStudyIdAndEntityIdResponse postStudiesEntitiesCountByStudyIdAndEntityId(
      String studyId, String entityId, EntityCountPostRequest rawRequest) {

    DataSource datasource = Resources.getApplicationDataSource();

    // unpack data from API input to model objects
    RequestBundle request = RequestBundle.unpack(datasource, studyId, entityId, rawRequest.getFilters(), Collections.emptyList());

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(
        request.getStudy().getEntityTree(), request.getFilters(), request.getTargetEntity());

    int count = StudySubsettingUtils.getEntityCount(
        datasource, prunedEntityTree, request.getTargetEntity(), request.getFilters());

    EntityCountPostResponse response = new EntityCountPostResponseImpl();
    response.setCount(count);

    return  PostStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

}
