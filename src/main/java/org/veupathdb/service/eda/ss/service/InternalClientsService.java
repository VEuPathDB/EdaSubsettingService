package org.veupathdb.service.eda.ss.service;

import jakarta.ws.rs.core.Context;
import org.glassfish.jersey.server.ContainerRequest;
import org.veupathdb.lib.container.jaxrs.server.annotations.Authenticated;
import org.veupathdb.service.eda.common.client.TabularResponseType;
import org.veupathdb.service.eda.generated.model.EntityTabularPostRequest;
import org.veupathdb.service.eda.generated.resources.SsInternalStudiesStudyIdEntitiesEntityIdTabular;

/**
 * Provides some of the same endpoints as regular StudiesService but without user authorization checking
 */
@Authenticated(allowGuests = true)
public class InternalClientsService implements SsInternalStudiesStudyIdEntitiesEntityIdTabular {

  @Context
  ContainerRequest _request;

  @Override
  public PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse postSsInternalStudiesEntitiesTabularByStudyIdAndEntityId(
      String studyId, String entityId, EntityTabularPostRequest requestBody) {
    return StudiesService.handleTabularRequest(_request, studyId, entityId, requestBody, false, (streamer, responseType) ->
      responseType == TabularResponseType.JSON
        ? PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithApplicationJson(streamer)
        : PostSsInternalStudiesEntitiesTabularByStudyIdAndEntityIdResponse
            .respond200WithTextTabSeparatedValues(streamer)
    );
  }

}
