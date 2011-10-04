/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2011 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.configuration;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.configuration.Property;
import org.sonar.core.config.ConfigurationUtils;
import org.sonar.jpa.session.DatabaseSessionFactory;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * Load settings from environment, conf/sonar.properties and database
 *
 * @since 2.12
 */
public class ServerSettings extends Settings {

  public static final String DEPLOY_DIR = "sonar.web.deployDir";

  private DatabaseSessionFactory sessionFactory;
  private Configuration deprecatedConfiguration;
  private File sonarHome;
  private File deployDir;

  public ServerSettings(PropertyDefinitions definitions, Configuration deprecatedConfiguration, ServletContext servletContext) {
    super(definitions);
    this.deprecatedConfiguration = deprecatedConfiguration;
    this.sonarHome = getSonarHome();
    this.deployDir = getDeployDir(servletContext);
    load();
  }

  public ServerSettings setSessionFactory(DatabaseSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
    return this;
  }

  public ServerSettings load() {
    clear();
    setProperty(CoreProperties.SONAR_HOME, sonarHome.getAbsolutePath());
    setProperty(DEPLOY_DIR, deployDir.getAbsolutePath());

    // order is important
    loadDatabaseSettings();
    addEnvironmentVariables();
    addSystemProperties();
    loadPropertiesFile();

    // update deprecated configuration
    ConfigurationUtils.copyToCommonsConfiguration(properties, deprecatedConfiguration);

    return this;
  }

  private void loadDatabaseSettings() {
    if (sessionFactory != null) {
      DatabaseSession session = sessionFactory.getSession();

      // Ugly workaround before the move to myBatis
      // Session is not up-to-date when Ruby on Rails inserts new rows in its own transaction. Seems like
      // Hibernate keeps a cache...
      session.commit();
      List<Property> properties = session.createQuery("from " + Property.class.getSimpleName() + " p where p.resourceId is null and p.userId is null").getResultList();

      for (Property property : properties) {
        setProperty(property.getKey(), property.getValue());
      }
    }
  }

  private void loadPropertiesFile() {
    File propertiesFile = new File(sonarHome, "conf/sonar.properties");
    if (!propertiesFile.isFile() || !propertiesFile.exists()) {
      throw new IllegalStateException("Properties file does not exist: " + propertiesFile);
    }

    try {
      Properties p = ConfigurationUtils.openProperties(propertiesFile);
      p = ConfigurationUtils.interpolateEnvVariables(p);
      addProperties(p);

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

  static File getSonarHome() {
    String home = System.getProperty("sonar.home");
    if (StringUtils.isBlank(home)) {
      home = System.getenv("SONAR_HOME");
      if (StringUtils.isBlank(home)) {
        Properties warProps = openWarProperties();
        home = warProps.getProperty("sonar.home");
      }
    }

    if (StringUtils.isBlank(home)) {
      throw new IllegalStateException("Please set location to SONAR_HOME");
    }

    File dir = new File(home);
    if (!dir.isDirectory() || !dir.exists()) {
      throw new IllegalStateException("SONAR_HOME is not valid: " + home);
    }
    return dir;
  }

  private static Properties openWarProperties() {
    try {
      InputStream input = ServerSettings.class.getResourceAsStream("/sonar-war.properties");
      return ConfigurationUtils.openInputStream(input);
    } catch (IOException e) {
      throw new IllegalStateException("Fail to load the file sonar-war.properties", e);
    }
  }
}
