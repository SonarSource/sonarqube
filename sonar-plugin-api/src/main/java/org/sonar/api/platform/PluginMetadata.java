package org.sonar.api.platform;

import java.io.File;
import java.util.List;

/**
 * @since 2.8
 */
public interface PluginMetadata {
  File getFile();

  List<File> getDeployedFiles();

  String getKey();

  String getName();

  String getMainClass();

  String getDescription();

  String getOrganization();

  String getOrganizationUrl();

  String getLicense();

  String getVersion();

  String getHomepage();

  boolean isUseChildFirstClassLoader();

  String getBasePlugin();

  boolean isCore();
}
