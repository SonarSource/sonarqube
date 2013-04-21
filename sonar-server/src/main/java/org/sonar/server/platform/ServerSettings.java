/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.platform;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.configuration.Configuration;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.core.config.ConfigurationUtils;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

/**
 * Load settings in the following order (the last override the first) :
 * <ol>
 * <li>general settings persisted in database</li>
 * <li>file $SONAR_HOME/conf/sonar.properties</li>
 * <li>environment variables</li>
 * <li>system properties</li>
 * </ol>
 *
 * @since 2.12
 */
public class ServerSettings extends Settings {

  public static final String DEPLOY_DIR = "sonar.web.deployDir";

  private Configuration deprecatedConfiguration;
  private File deployDir;
  private File sonarHome;

  public ServerSettings(PropertyDefinitions definitions, Configuration deprecatedConfiguration, ServletContext servletContext) {
    this(definitions, deprecatedConfiguration, getDeployDir(servletContext), SonarHome.getHome());
  }

  @VisibleForTesting
  ServerSettings(PropertyDefinitions definitions, Configuration deprecatedConfiguration, File deployDir, File sonarHome) {
    super(definitions);
    this.deprecatedConfiguration = deprecatedConfiguration;
    this.deployDir = deployDir;
    this.sonarHome = sonarHome;
    load(Collections.<String, String>emptyMap());
  }

  public ServerSettings activateDatabaseSettings(Map<String, String> databaseProperties) {
    return load(databaseProperties);
  }

  private ServerSettings load(Map<String, String> databaseSettings) {
    clear();
    setProperty(CoreProperties.SONAR_HOME, sonarHome.getAbsolutePath());
    setProperty(DEPLOY_DIR, deployDir.getAbsolutePath());

    // order is important : the last override the first
    addProperties(databaseSettings);
    loadPropertiesFile(sonarHome);
    addEnvironmentVariables();
    addSystemProperties();

    return this;
  }

  private void loadPropertiesFile(File sonarHome) {
    File propertiesFile = new File(sonarHome, "conf/sonar.properties");
    if (!propertiesFile.isFile() || !propertiesFile.exists()) {
      throw new IllegalStateException("Properties file does not exist: " + propertiesFile);
    }
    try {
      Properties p = ConfigurationUtils.openProperties(propertiesFile);
      addProperties(ConfigurationUtils.interpolateEnvVariables(p));
    } catch (Exception e) {
      throw new IllegalStateException("Fail to load configuration file: " + propertiesFile, e);
    }
  }

  static File getDeployDir(ServletContext servletContext) {
    String dirname = servletContext.getRealPath("/deploy/");
    if (dirname == null) {
      throw new IllegalArgumentException("Web app directory not found : /deploy/");
    }
    File dir = new File(dirname);
    if (!dir.exists()) {
      throw new IllegalArgumentException("Web app directory does not exist: " + dir);
    }
    return dir;
  }

  @Override
  protected void doOnSetProperty(String key, @Nullable String value) {
    deprecatedConfiguration.setProperty(key, value);
  }

  @Override
  protected void doOnRemoveProperty(String key) {
    deprecatedConfiguration.clearProperty(key);
  }

  @Override
  protected void doOnClearProperties() {
    deprecatedConfiguration.clear();
  }
}
