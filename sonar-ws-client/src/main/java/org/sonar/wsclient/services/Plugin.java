package org.sonar.wsclient.services;

/**
 * @since 2.4
 */
public class Plugin extends Model {

  private String key;
  private String name;
  private String version;

  public String getKey() {
    return key;
  }

  public Plugin setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public Plugin setName(String name) {
    this.name = name;
    return this;
  }

  public String getVersion() {
    return version;
  }

  public Plugin setVersion(String version) {
    this.version = version;
    return this;
  }

}
