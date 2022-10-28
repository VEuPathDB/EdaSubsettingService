package org.veupathdb.service.eda.ss;

import org.gusdb.fgputil.db.slowquery.QueryLogConfig;
import org.gusdb.fgputil.db.slowquery.QueryLogger;
import org.veupathdb.lib.container.jaxrs.config.Options;
import org.veupathdb.lib.container.jaxrs.server.ContainerResources;
import org.veupathdb.lib.container.jaxrs.server.Server;
import org.veupathdb.lib.container.jaxrs.server.middleware.PrometheusFilter;

public class Main extends Server {
  private static final String EDA_ID_REGEX = "[A-Z0-9_-]+";

  public static void main(String[] args) {
    new Main().start(args);
  }

  public Main() {
    PrometheusFilter.setPathTransform(path ->
        path.replace("studies/" + EDA_ID_REGEX, "studies/{study-id}")
            .replace("entities/" + EDA_ID_REGEX, "entities/{entity-id}")
            .replace("variables/" + EDA_ID_REGEX, "variables/{variable-id}"));
    QueryLogger.initialize(new QLF(){});
  }

  @Override
  protected ContainerResources newResourceConfig(Options options) {
    return new Resources(options);
  }

  public static class QLF implements QueryLogConfig {
    public double getBaseline() {
      return 0.05D;
    }

    public double getSlow() {
      return 1.0D;
    }

    public boolean isIgnoredSlow(String sql) {
      return false;
    }

    public boolean isIgnoredBaseline(String sql) {
      return false;
    }
  }

}
