package org.sonar.wsclient.services;

/**
 * @since 2.4
 */
public class UpdateCenterQuery extends Query<Plugin> {

  public static final String BASE_URL = "/api/updatecenter/";
  private String action;

  private UpdateCenterQuery(String action) {
    this.action = action;
  }

  @Override
  public Class<Plugin> getModelClass() {
    return Plugin.class;
  }

  @Override
  public String getUrl() {
    return BASE_URL + action;
  }

  public static UpdateCenterQuery createForInstalledPlugins() {
    return new UpdateCenterQuery("installed_plugins");
  }

}
