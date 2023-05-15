package org.veupathdb.service.eda.ss.service;

import java.util.Date;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.service.eda.generated.resources.ClearMetadataCache;
import org.veupathdb.service.eda.ss.Resources;

public class ClearMetadataCacheService implements ClearMetadataCache {

  private static final Logger LOG = LogManager.getLogger(ClearMetadataCacheService.class);

  @Override
  public GetClearMetadataCacheResponse getClearMetadataCache() {
    Resources.getMetadataCache().clear();
    String message = "Cache successfully cleared at " + new Date();
    LOG.info(message);
    return GetClearMetadataCacheResponse.respond200WithTextPlain(message);
  }

}
