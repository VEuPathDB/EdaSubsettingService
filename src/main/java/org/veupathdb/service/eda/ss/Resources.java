package org.veupathdb.service.eda.ss;

import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SingleLongResultSetHandler;
import org.gusdb.fgputil.functional.Functions;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.ss.service.ClearMetadataCacheService;
import org.veupathdb.service.eda.ss.service.InternalClientsService;
import org.veupathdb.service.eda.ss.service.StudiesService;
import org.veupathdb.service.eda.ss.test.StubDb;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {

  private static final Logger LOG = LogManager.getLogger(Resources.class);

  public static EnvironmentVars ENV = new EnvironmentVars();

  // use in-memory test DB unless "real" application DB is configured
  private static boolean USE_IN_MEMORY_TEST_DATABASE = true;

  // TEMPORARY KLUGE!!! Sets whether to apply a transformation from
  //   entity type (assay) into the hasCollections property (true)
  private static Boolean CONVERT_ASSAYS_TO_HAS_COLLECTIONS;

  public Resources(Options opts) {
    super(opts);
    ENV.load();

    // initialize auth and required DBs
    DbManager.initUserDatabase(opts);
    DbManager.initAccountDatabase(opts);
    enableAuth();

    if (opts.getAppDbOpts().name().isPresent() ||
        opts.getAppDbOpts().tnsName().isPresent()) {
      // application database configured; use it
      USE_IN_MEMORY_TEST_DATABASE = false;
    }

    if (ENV.isDevelopmentMode()) {
      enableJerseyTrace();
    }

    if (!USE_IN_MEMORY_TEST_DATABASE) {
      DbManager.initApplicationDatabase(opts);
      LOG.info("Using application DB connection URL: " +
          DbManager.getInstance().getApplicationDatabase().getConfig().getConnectionUrl());
    }
  }

  public static synchronized boolean getConvertAssaysFlag() {
    if (CONVERT_ASSAYS_TO_HAS_COLLECTIONS == null) {
      // not yet calculated; do so
      if (USE_IN_MEMORY_TEST_DATABASE) {
        CONVERT_ASSAYS_TO_HAS_COLLECTIONS = false;
      }
      else {
        // count rows in projectinfo where project name is mbio
        String sql = "select count(*) from core.projectinfo where name = 'MicrobiomeDB'";
        // if any rows exist, set assay entities' hasCollections flag to true
        CONVERT_ASSAYS_TO_HAS_COLLECTIONS =
            Functions.wrapException(() -> new SQLRunner(getApplicationDataSource(), sql)
                .executeQuery(new SingleLongResultSetHandler()).orElseThrow() > 0);
        LOG.info("Setting CONVERT_ASSAYS_TO_HAS_COLLECTIONS to " + CONVERT_ASSAYS_TO_HAS_COLLECTIONS);
      }
    }
    return CONVERT_ASSAYS_TO_HAS_COLLECTIONS;
  }

  public static DataSource getApplicationDataSource() {
    return USE_IN_MEMORY_TEST_DATABASE
      ? StubDb.getDataSource()
      : DbManager.applicationDatabase().getDataSource();
  }

  public static String getAppDbSchema() {
    return USE_IN_MEMORY_TEST_DATABASE ? "" : ENV.getAppDbSchema();
  }

  public static String getUserStudySchema() {
    return USE_IN_MEMORY_TEST_DATABASE ? "" : ENV.getUserStudySchema();
  }

  /**
   * Returns an array of JaxRS endpoints, providers, and contexts.
   *
   * Entries in the array can be either classes or instances.
   */
  @Override
  protected Object[] resources() {
    return new Object[] {
      StudiesService.class,
      InternalClientsService.class,
      ClearMetadataCacheService.class
    };
  }
}
