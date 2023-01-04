package org.veupathdb.service.eda.ss.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
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
import org.veupathdb.service.eda.common.client.DatasetAccessClient;
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
import org.veupathdb.service.eda.generated.model.ValueSpec;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostRequest;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponse;
import org.veupathdb.service.eda.generated.model.VariableDistributionPostResponseImpl;
import org.veupathdb.service.eda.generated.resources.Studies;
import org.veupathdb.service.eda.ss.Resources;
import org.veupathdb.service.eda.ss.model.Entity;
import org.veupathdb.service.eda.ss.model.Study;
import org.veupathdb.service.eda.ss.model.StudyOverview;
import org.veupathdb.service.eda.ss.model.db.FilteredResultFactory;
import org.veupathdb.service.eda.ss.model.db.StudyFactory;
import org.veupathdb.service.eda.ss.model.db.StudyProvider;
import org.veupathdb.service.eda.ss.model.db.StudyResolver;
import org.veupathdb.service.eda.ss.model.distribution.DistributionFactory;
import org.veupathdb.service.eda.ss.model.reducer.BinaryValuesStreamer;
import org.veupathdb.service.eda.ss.model.tabular.DataSourceType;
import org.veupathdb.service.eda.ss.model.tabular.TabularReportConfig;
import org.veupathdb.service.eda.ss.model.tabular.TabularResponses;
import org.veupathdb.service.eda.ss.model.variable.Variable;
import org.veupathdb.service.eda.ss.model.variable.VariableWithValues;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.MultiPathStudyFinder;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;

import static org.veupathdb.service.eda.ss.service.ApiConversionUtil.*;

@Authenticated(allowGuests = true)
public class StudiesService implements Studies {
  private static final Logger LOG = LogManager.getLogger(StudiesService.class);

  private static final long MAX_ROWS_FOR_SINGLE_PAGE_ACCESS = 20;

  @Context
  ContainerRequest _request;

  @Override
  public GetStudiesResponse getStudies() {
    // get IDs of studies visible to this user
    Set<String> visibleStudies = new DatasetAccessClient(
        Resources.ENV.getDatasetAccessServiceUrl(),
        UserProvider.getSubmittedAuth(_request).orElseThrow()
    ).getStudyDatasetInfoMapForUser().keySet();

    // filter overviews by visible studies
    List<StudyOverview> visibleOverviews = getStudyResolver()
        .getStudyOverviews().stream()
        .filter(overview -> visibleStudies.contains(overview.getStudyId()))
        .collect(Collectors.toList());

    // convert to API objects and return
    StudiesGetResponse out = new StudiesGetResponseImpl();
    out.setStudies(ApiConversionUtil.toApiStudyOverviews(visibleOverviews));
    return GetStudiesResponse.respond200WithApplicationJson(out);
  }

  @Override
  public GetStudiesByStudyIdResponse getStudiesByStudyId(String studyId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    Study study = getStudyResolver().getStudyById(studyId);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(study);
    StudyIdGetResponse response = new StudyIdGetResponseImpl();
    response.setStudy(apiStudyDetail);
    return GetStudiesByStudyIdResponse.respond200WithApplicationJson(response);
  }

  @Override
  public GetStudiesEntitiesByStudyIdAndEntityIdResponse getStudiesEntitiesByStudyIdAndEntityId(String studyId, String entityId) {
    checkPerms(_request, studyId, StudyAccess::allowStudyMetadata);
    APIStudyDetail apiStudyDetail = ApiConversionUtil.getApiStudyDetail(getStudyResolver().getStudyById(studyId));
    APIEntity entity = findEntityById(apiStudyDetail.getRootEntity(), entityId).orElseThrow(NotFoundException::new);
    EntityIdGetResponse response = new EntityIdGetResponseImpl();
    // copy properties of found entity, skipping children
    response.setId(entity.getId());
    response.setDescription(entity.getDescription());
    response.setDisplayName(entity.getDisplayName());
    response.setDisplayNamePlural(entity.getDisplayNamePlural());
    response.setVariables(entity.getVariables());
    response.setCollections(entity.getCollections());
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
      Study study = getStudyResolver().getStudyById(studyId);
      String dataSchema = resolveSchema(study);

      // unpack data from API input to model objects
      RequestBundle req = RequestBundle.unpack(dataSchema, study, entityId, request.getFilters(), ListBuilder.asList(variableId), null);

      // FIXME: need this until we turn on schema-level checking to enforce requiredness
      if (request.getValueSpec() == null) request.setValueSpec(ValueSpec.COUNT);

      DistributionResult result = DistributionFactory.processDistributionRequest(
          Resources.getApplicationDataSource(), dataSchema, req.getStudy(), req.getTargetEntity(),
          getRequestedVariable(req), req.getFilters(), toInternalValueSpec(request.getValueSpec()),
          toInternalBinSpecWithRange(request.getBinSpec()));

      VariableDistributionPostResponse response = new VariableDistributionPostResponseImpl();
      response.setHistogram(toApiHistogramBins(result.getHistogramData()));
      response.setStatistics(toApiHistogramStats(result.getStatistics()));

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
      responseType == TabularResponses.Type.JSON
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
      String entityDisplay = getStudyResolver().getStudyById(studyId).getEntity(entityId).orElseThrow().getDisplayName();
      String fileName = studyId + "_" + entityDisplay + "_subsettedData.txt";
      String dispositionHeaderValue = "attachment; filename=\"" + fileName + "\"";
      _request.setProperty(CustomResponseHeadersFilter.CUSTOM_HEADERS_KEY,
          new MapBuilder<>(HttpHeaders.CONTENT_DISPOSITION, dispositionHeaderValue).toMap());
      return typedResponse;
    }
    catch (JsonProcessingException e) {
      throw new BadRequestException(e.getMessage());
    }
  }

  public static <T> T handleTabularRequest(
      ContainerRequest requestContext, String studyId, String entityId,
      EntityTabularPostRequest requestBody, boolean checkUserPermissions,
      BiFunction<EntityTabularPostResponseStream,TabularResponses.Type,T> responseConverter) {
    LOG.info("Handling tabular request for study {} and entity {}.", studyId, entityId);
    Study study = getStudyResolver().getStudyById(studyId);
    String dataSchema = resolveSchema(study);
    RequestBundle request = RequestBundle.unpack(dataSchema, study, entityId, requestBody.getFilters(), requestBody.getOutputVariableIds(), requestBody.getReportConfig());

    if (checkUserPermissions) {
      // if requested, make sure user has permission to access this amount of tabular data (may differ based on report config)
      checkPerms(requestContext, studyId, getTabularAccessPredicate(request.getReportConfig()));
    }

    TabularResponses.Type responseType = TabularResponses.Type.fromAcceptHeader(requestContext);
    final BinaryFilesManager binaryFilesManager = new BinaryFilesManager(
        new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));
    final BinaryValuesStreamer binaryValuesStreamer = new BinaryValuesStreamer(binaryFilesManager,
            Resources.getFileChannelThreadPool(), Resources.getDeserializerThreadPool());
    if (shouldRunFileBasedSubsetting(request, binaryFilesManager)) {
      LOG.info("Running file-based subsetting for study " + studyId);
      EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream(outStream ->
          FilteredResultFactory.produceTabularSubsetFromFile(request.getStudy(), request.getTargetEntity(),
              request.getRequestedVariables(), request.getFilters(), responseType.getBinaryFormatter(),
              request.getReportConfig(), outStream, binaryValuesStreamer));
      return responseConverter.apply(streamer, responseType);
    }
    LOG.info("Performing oracle-based subsetting for study " + studyId);
    EntityTabularPostResponseStream streamer = new EntityTabularPostResponseStream(outStream ->
        FilteredResultFactory.produceTabularSubset(Resources.getApplicationDataSource(), dataSchema,
            request.getStudy(), request.getTargetEntity(), request.getRequestedVariables(), request.getFilters(),
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

  /**
   * Skip and do oracle-based subsetting if either:
   * 1. File-based subsetting is disabled via environment variable
   * 2. getReportConfig().getDataSourceType() is not specified or is specified as DATABASE
   * 3. Any data is missing in files (i.e. MissingDataException is thrown).
   **/
  private static boolean shouldRunFileBasedSubsetting(RequestBundle requestBundle, BinaryFilesManager binaryFilesManager) {
    if (!Resources.isFileBasedSubsettingEnabled() && requestBundle.getReportConfig().getDataSourceType() != DataSourceType.FILES) {
      return false;
    }
    if (!binaryFilesManager.studyHasFiles(requestBundle.getStudy())) {
      LOG.info("Unable to find study dir for " + requestBundle.getStudy().getStudyId() + " in study files.");
      return false;
    }
    if (!binaryFilesManager.entityDirExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find entity dir for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    if (!binaryFilesManager.idMapFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find ID file for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    if (!requestBundle.getTargetEntity().getAncestorEntities().isEmpty() && !binaryFilesManager.ancestorFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity())) {
      LOG.info("Unable to find ancestor file for " + requestBundle.getTargetEntity().getId() + " in study files.");
      return false;
    }
    for (VariableWithValues outputVar: requestBundle.getRequestedVariables()) {
      if (!binaryFilesManager.variableFileExists(requestBundle.getStudy(), requestBundle.getTargetEntity(), outputVar)) {
        LOG.info("Unable to find output var " + outputVar.getId() + " in study files.");
        return false;
      }
    }
    List<VariableWithValues> filterVars = requestBundle.getFilters().stream()
        .flatMap(filter -> filter.getAllVariables().stream())
        .collect(Collectors.toList());
    for (VariableWithValues filterVar: filterVars) {
      if (!binaryFilesManager.variableFileExists(requestBundle.getStudy(), filterVar.getEntity(), filterVar)) {
        LOG.info("Unable to find filterVar var " + filterVar.getId() + " in study files.");
        return false;
      }
    }
    return true;
  }

  @Override
  public PostStudiesEntitiesCountByStudyIdAndEntityIdResponse postStudiesEntitiesCountByStudyIdAndEntityId(
      String studyId, String entityId, EntityCountPostRequest rawRequest) {
    LOG.info("Handling count request.");

    checkPerms(_request, studyId, StudyAccess::allowSubsetting);
    Study study = getStudyResolver().getStudyById(studyId);
    String dataSchema = resolveSchema(study);

    // unpack data from API input to model objects
    RequestBundle request = RequestBundle.unpack(dataSchema, study, entityId, rawRequest.getFilters(), Collections.emptyList(), null);

    TreeNode<Entity> prunedEntityTree = FilteredResultFactory.pruneTree(
        request.getStudy().getEntityTree(), request.getFilters(), request.getTargetEntity());

    final BinaryFilesManager binaryFilesManager = new BinaryFilesManager(
        new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));

    final BinaryValuesStreamer binaryValuesStreamer = new BinaryValuesStreamer(binaryFilesManager,
            Resources.getFileChannelThreadPool(), Resources.getDeserializerThreadPool());

    EntityCountPostResponse response = new EntityCountPostResponseImpl();
    if (shouldRunFileBasedSubsetting(request, binaryFilesManager)) {
      long count = FilteredResultFactory.getEntityCount(prunedEntityTree, request.getTargetEntity(), request.getFilters(),
              binaryValuesStreamer, study);
      response.setCount(count);
    } else {
      long count = FilteredResultFactory.getEntityCount(
          Resources.getApplicationDataSource(), dataSchema, prunedEntityTree, request.getTargetEntity(), request.getFilters());
      response.setCount(count);
    }

    return PostStudiesEntitiesCountByStudyIdAndEntityIdResponse.respond200WithApplicationJson(response);
  }

  private static void checkPerms(ContainerRequest request, String studyId, Predicate<StudyAccess> accessPredicate) {
    Entry<String, String> authHeader = UserProvider.getSubmittedAuth(request).orElseThrow();
    StudyAccess.confirmPermission(authHeader, Resources.ENV.getDatasetAccessServiceUrl(), studyId, accessPredicate);
  }

  private static StudyProvider getStudyResolver() {
    return new StudyResolver(
        MetadataCache.instance(),
        new StudyFactory(
            Resources.getApplicationDataSource(),
            Resources.getUserStudySchema(),
            StudyOverview.StudySourceType.USER_SUBMITTED
        )
    );
  }

  private static String resolveSchema(Study study) {
    return switch(study.getStudySourceType()) {
      case USER_SUBMITTED -> Resources.getUserStudySchema();
      case CURATED -> Resources.getAppDbSchema();
    };
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

  private VariableWithValues<?> getRequestedVariable(RequestBundle req) {
    if (req.getRequestedVariables().isEmpty()) {
      throw new RuntimeException("No requested variables (empty URL segment?)");
    }
    Variable var = req.getRequestedVariables().get(0);
    if (!(var instanceof VariableWithValues)) {
      throw new BadRequestException("Distribution endpoint can only be called with a variable that has values.");
    }
    return (VariableWithValues<?>)var;
  }
}
