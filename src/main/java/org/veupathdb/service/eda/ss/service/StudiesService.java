package org.veupathdb.service.eda.ss.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import javax.sql.DataSource;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.server.ContainerRequest;
import org.gusdb.fgputil.ListBuilder;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.distribution.DistributionResult;
import org.gusdb.fgputil.functional.TreeNode;
import org.gusdb.fgputil.json.JsonUtil;
import org.gusdb.fgputil.web.UrlEncodedForm;
import org.veupathdb.lib.container.jaxrs.providers.UserProvider;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.lib.container.jaxrs.server.middleware.CustomResponseHeadersFilter;
import org.veupathdb.service.eda.common.auth.StudyAccess;
import org.veupathdb.service.eda.common.client.TabularResponseType;
import org.veupathdb.service.eda.generated.model.APIEntity;
import org.veupathdb.service.eda.generated.model.APIStudyDetail;
import org.veupathdb.service.eda.generated.model.EntityCountPostRequest;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponse;
import org.veupathdb.service.eda.generated.model.EntityCountPostResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponse;
import org.veupathdb.service.eda.generated.model.EntityIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.model.EntityTabularPostResponseStream;
import org.veupathdb.service.eda.generated.model.StudiesGetResponse;
import org.veupathdb.service.eda.generated.model.StudiesGetResponseImpl;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponse;
import org.veupathdb.service.eda.generated.model.StudyIdGetResponseImpl;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponse;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponseImpl;
import org.veupathdb.service.eda.generated.resources.Studies;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.MetadataCache;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudySubsettingUtils;
import org.veupathdb.service.eda.ss.model.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.distribution.DistributionFactory;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;

@Authenticated(allowGuests = true)
public class StudiesService implements Studies {

  private static final Logger LOG = LogManager.getLogger(StudiesService.class);

  private static final long MAX_ROWS_FOR_SINGLE_PAGE_ACCESS = 20;

  @Context
  ContainerRequest _request;

  @Override
  public GetStudiesResponse getStudies() {
    StudiesGetResponse out = new StudiesGetResponseImpl();
    out.setStudies(MetadataCache.getStudyOverviews());
    return GetStudiesResponse.respond200WithApplicationJson(out);
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    Study study = MetadataCache.getStudy(studyId);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(study);
    StudyIdGetResponse response = new StudyIdGetResponseImpl();
    response.setStudy(apiStudyDetail);
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(MetadataCache.getStudy(studyId));
    APIEntity entity = findEntityById(apiStudyDetail.getRootEntity(), entityId).orElseThrow(NotFoundException::new);
    EntityIdGetResponse response = new EntityIdGetResponseImpl();
    // copy properties of found entity, skipping children
    response.setId(entity.getId());
    response.setDescription(entity.getDescription());
    response.setDisplayName(entity.getDisplayName());
    response.setDisplayNamePlural(entity.getDisplayNamePlural());
    response.setVariables(entity.getVariables());
    for (Entry<String,Object> prop : entity.getAdditionalProperties().entrySet()) {
      response.setAdditionalProperties(prop.getKey(), prop.getValue());
    }
    return GetStudiesEntitiesByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse 
  postStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableId(
      String studyId, String entityId, String variableId, VariableDistributionPostRequest request) {
    try {
      checkPerms(_request, studyId, StudyAccess::allowSubsetting);

      // unpack data from API input to model objects
      DataSource ds = Resources.getApplicationDataSource();
      RequestBundle req = RequestBundle.unpack(ds, studyId, entityId, request.getFilters(), ListBuilder.asList(variableId), null);
      VariableWithValues var = getRequestedVariable(req);

      DistributionResult result = DistributionFactory.processDistributionRequest(ds, req.getStudy(),
          req.getTargetEntity(), var, req.getFilters(), request.getValueSpec(),
          Optional.ofNullable(request.getBinSpec()));

      VariableDistributionPostResponse response = new VariableDistributionPostResponseImpl();
      response.setHistogram(ApiConversionUtil.toApiHistogramBins(result.getHistogramData()));
      response.setStatistics(ApiConversionUtil.toApiHistogramStats(result.getStatistics()));

      return PostStudiesEntitiesVariablesDistributionByStudyIdAndEntityIdAndVariableIdResponse.
          respond200WithApplicationJson(response);
    }
    catch (RuntimeException e) {
      LOG.error("Unable to deliver distribution response", e);
      throw e;
    }
  }

  @Override
  public PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse postStudiesEntitiesTabularByStudyIdAndEntityId(String studyId,
      String entityId, EntityTabularPostRequest requestBody) {
    return handleTabularRequest(_request, studyId, entityId, requestBody, true, (streamer, responseType) ->
      responseType == TabularResponseType.JSON
        ? PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithApplicationJson(streamer)
        : PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithTextTabSeparatedValues(streamer)
    );
  }

  @Override
  public PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse postStudiesEntitiesTabularByStudyIdAndEntityId(String studyId, String entityId) {
    UrlEncodedForm form = new UrlEncodedForm(_request.getEntityStream());
    String requestJson = form.getFirstParamValue("data")
        .orElseThrow(() -> new BadRequestException(
            "Form must contain parameter 'data' containing tabular request JSON."));
    try {
      EntityTabularPostRequest request = JsonUtil.Jackson.readValue(requestJson, EntityTabularPostRequest.class);
      PostStudiesEntitiesTabularByStudyIdAndEntityIdResponse typedResponse =
          postStudiesEntitiesTabularByStudyIdAndEntityId(studyId, entityId, request);
      // success so far; add header to response
      String entityDisplay = MetadataCache.getStudy(studyId).getEntity(entityId).get().getDisplayName();
      String fileName = studyId + "_" + entityDisplay + "_subsettedData.txt";
      String dispositionHeaderValue = "attachment; filename=\"" + fileName + "\"";
      _request.setProperty(CustomResponseHeadersFilter.CUSTOM_HEADERS_KEY,
          new MapBuilder<String,String>(HttpHeaders.CONTENT_DISPOSITION, dispositionHeaderValue).toMap());
      return typedResponse;
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  public static <T> T handleTabularRequest(
      ContainerRequest requestContext, String studyId, String entityId,
      EntityTabularPostRequest requestBody, boolean checkUserPermissions,
      BiFunction<EntityTabularPostResponseStream,TabularResponseType,T> responseConverter) {

    DataSource dataSource = Resources.getApplicationDataSource();
    RequestBundle request = RequestBundle.unpack(dataSource, studyId, entityId, requestBody.getFilters(), requestBody.getOutputVariableIds(), requestBody.getReportConfig());

    if (checkUserPermissions) {
      // if requested, make sure user has permission to access this amount of tabular data (may differ based on report config)
      checkPerms(requestContext, studyId, getTabularAccessPredicate(request.getReportConfig()));
    }

    TabularResponseType responseType = TabularResponseType.fromAcceptHeader(requestContext);

    EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream
        (outStream -> StudySubsettingUtils.produceTabularSubset(dataSource, request.getStudy(),
            request.getTargetEntity(), request.getRequestedVariables(), request.getFilters(),
            request.getReportConfig(), responseType.getFormatter(), outStream));

    return responseConverter.apply(streamer, responseType);
  }

  private static Predicate<StudyAccess> getTabularAccessPredicate(TabularReportConfig reportConfig) {
    // trigger single-page access IFF user specifies paging with zero offset and number of rows under the single-page max
    if (reportConfig.getOffset() == 0L &&
        reportConfig.getNumRows().isPresent() &&
        reportConfig.getNumRows().get() <= MAX_ROWS_FOR_SINGLE_PAGE_ACCESS) {
      return StudyAccess::allowResultsFirstPage;
    }
    // if paging not present or does not meet single-page criteria, user needs all-results access
    return StudyAccess::allowResultsAll;
  }

  @Override
  public PostStudiesEntitiesCountByStudyIdAndEntityIdResponse postStudiesEntitiesCountByStudyIdAndEntityId(
      String studyId, String entityId, EntityCountPostRequest rawRequest) {
    checkPerms(_request, studyId, StudyAccess::allowSubsetting);

    DataSource datasource = Resources.getApplicationDataSource();

    // unpack data from API input to model objects
    RequestBundle request = RequestBundle.unpack(datasource, studyId, entityId, rawRequest.getFilters(), Collections.emptyList(), null);

    TreeNode<Entity> prunedEntityTree = StudySubsettingUtils.pruneTree(
        request.getStudy().getEntityTree(), request.getFilters(), request.getTargetEntity());

    long count = StudySubsettingUtils.getEntityCount(
        datasource, prunedEntityTree, request.getTargetEntity(), request.getFilters());

    EntityCountPostResponse response = new EntityCountPostResponseImpl();
    response.setCount(count);

    return  PostStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

  private static void checkPerms(ContainerRequest request, String studyId, Predicate<StudyAccess> accessPredicate) {
    Entry<String, String> authHeader = UserProvider.getSubmittedAuth(request).orElseThrow();
    StudyAccess.confirmPermission(authHeader, Resources.ENV.getDatasetAccessServiceUrl(), studyId, accessPredicate);
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

}
