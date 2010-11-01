package org.sonar.wsclient.services;

/**
 * @since 2.4
 */
public class PluginQuery extends Query<Plugin> {

  public static final String BASE_URL = "/api/plugins";

  @Override
  public Class<Plugin> getModelClass() {
    return Plugin.class;
  }

  @Override
  public String getUrl() {
    return BASE_URL;
  }
}
