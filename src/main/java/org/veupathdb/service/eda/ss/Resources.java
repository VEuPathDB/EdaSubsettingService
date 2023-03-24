package org.veupathdb.service.eda.ss;

import javax.sql.DataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.utils.db.DbManager;
import org.veupathdb.service.eda.ss.model.variable.binary.BinaryFilesManager;
import org.veupathdb.service.eda.ss.model.variable.binary.SimpleStudyFinder;
import org.veupathdb.service.eda.ss.service.ClearMetadataCacheService;
import org.veupathdb.service.eda.ss.service.InternalClientsService;
import org.veupathdb.service.eda.ss.service.StudiesService;
import org.veupathdb.service.eda.ss.test.StubDb;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

  private static final ExecutorService FILE_READ_THREAD_POOL = Executors.newCachedThreadPool();

  private static final ExecutorService DESERIALIZER_THREAD_POOL = Executors.newFixedThreadPool(16);

  private static final BinaryFilesManager BINARY_FILES_MANAGER = new BinaryFilesManager(
      new SimpleStudyFinder(Resources.getBinaryFilesDirectory().toString()));

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

  public static BinaryFilesManager getBinaryFilesManager() {
    return BINARY_FILES_MANAGER;
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
    return Path.of(ENV.getBinaryFilesMount(), ENV.getBinaryFilesDirectory().replace("%DB_BUILD%", ENV.getDbBuild()));
  }

  public static ExecutorService getFileChannelThreadPool() {
    return FILE_READ_THREAD_POOL;
  }

  public static ExecutorService getDeserializerThreadPool() {
    return DESERIALIZER_THREAD_POOL;
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
