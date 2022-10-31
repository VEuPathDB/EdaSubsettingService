package org.veupathdb.service.eda.ss;

import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.ss.service.ClearMetadataCacheService;
import org.veupathdb.service.eda.ss.service.InternalClientsService;
import org.veupathdb.service.eda.ss.service.StudiesService;
import org.veupathdb.service.eda.ss.test.StubDb;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

  public static boolean isFileBasedSubsettingEnabled() {
    return ENV.isFileBasedSubsettingEnabled();
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

  public static Path getBinaryFilesDirectory() {
    return Path.of(ENV.getBinaryFilesDirectory());
  }

  public static List<String> getAvailableBinaryFilesPaths() {
    return Arrays.stream(ENV.getAvailableBinaryFilesPaths().split(";"))
        .map(env -> env.replaceAll("%DB_BUILD%", ENV.getDbBuild()))
        .collect(Collectors.toList());
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
