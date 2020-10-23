package org.veupathdb.service.edass;

import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.service.edass.service.Studies;
import org.veupathdb.service.edass.stubdb.StubDb;

import javax.sql.DataSource;

/**
 * Service Resource Registration.
 *
 * This is where all the individual service specific resources and middleware
 * should be registered.
 */
public class Resources extends ContainerResources {
  public Resources(Options opts) {
    super(opts);
  }

  /**
   * Returns an array of JaxRS endpoints, providers, and contexts.
   *
   * Entries in the array can be either classes or instances.
   */
  @Override
  protected Object[] resources() {
    return new Object[] {
      Studies.class,
    };
  }

  public static DataSource getApplicationDataSource() {
    return StubDb.getDataSource();
  }
}
