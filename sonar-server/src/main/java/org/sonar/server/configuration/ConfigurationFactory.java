/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
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

import org.apache.commons.configuration.*;
import org.apache.commons.configuration.interpol.ConfigurationInterpolator;
import org.apache.commons.lang.text.StrLookup;

import javax.servlet.ServletContextEvent;
import java.io.File;
import java.util.HashMap;

public final class ConfigurationFactory {

  public ConfigurationFactory() {
    allowUsingEnvironmentVariables();
  }

  public Configuration getConfiguration(ServletContextEvent sce) {
    CoreConfiguration configuration = new CoreConfiguration();
    configuration.addConfiguration(getConfigurationFromPropertiesFile());
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(getDirectoriesConfiguration(sce));
    return configuration;
  }

  private void allowUsingEnvironmentVariables() {
    ConfigurationInterpolator.registerGlobalLookup("env", new StrLookup() {
      @Override
      public String lookup(String varName) {
        return System.getenv(varName);
      }
    });
  }

  private Configuration getDirectoriesConfiguration(ServletContextEvent sce) {
    MapConfiguration result = new MapConfiguration(new HashMap());
    String webAppDir =  autodetectWebappDeployDirectory(sce);
    result.setProperty(CoreConfiguration.DEPLOY_DIR, webAppDir);
    return result;
  }

  protected PropertiesConfiguration getConfigurationFromPropertiesFile(String filename) throws ConfigurationException {
    try {
      return new PropertiesConfiguration(ConfigurationFactory.class.getResource(filename));

    } catch (org.apache.commons.configuration.ConfigurationException e) {
      throw new ConfigurationException("can not load the file " + filename + " from classpath", e);
    }
  }

  public PropertiesConfiguration getConfigurationFromPropertiesFile() throws ConfigurationException {
    return getConfigurationFromPropertiesFile("/conf/sonar.properties");
  }

  protected String autodetectWebappDeployDirectory(ServletContextEvent sce) {
    String webAppPublicDirPath = sce.getServletContext().getRealPath("/deploy/");
    if (webAppPublicDirPath == null) {
      throw new ConfigurationException("Web app directory not found : /deploy/");
    }
    File file = new File(webAppPublicDirPath);
    if (!file.exists()) {
      throw new ConfigurationException("Web app directory not found : " + file);
    }
    return file.toString();
  }

}
