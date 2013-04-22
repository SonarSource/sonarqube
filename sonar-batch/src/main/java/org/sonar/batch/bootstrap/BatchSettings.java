/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.bootstrap;

import com.google.common.collect.Maps;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONValue;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class BatchSettings extends Settings {
  private Configuration deprecatedConfiguration;

  // Keep module settings for initialization of ProjectSettings
  // module key -> <key,val>
  private Map<String, Map<String, String>> moduleProperties = Maps.newHashMap();

  public BatchSettings(BootstrapSettings bootstrapSettings, PropertyDefinitions propertyDefinitions,
                       ServerClient client, Configuration deprecatedConfiguration, BootstrapProperties globalProperties) {
    this(bootstrapSettings, propertyDefinitions, client, deprecatedConfiguration, globalProperties, null);
  }

  public BatchSettings(BootstrapSettings bootstrapSettings, PropertyDefinitions propertyDefinitions,
                       ServerClient client, Configuration deprecatedConfiguration, BootstrapProperties bootstrapProperties,
                       @Nullable ProjectReactor projectReactor) {
    super(propertyDefinitions);
    this.deprecatedConfiguration = deprecatedConfiguration;
    init(bootstrapSettings, client, bootstrapProperties, projectReactor);
  }

  private void init(BootstrapSettings bootstrapSettings, ServerClient client, BootstrapProperties bootstrapProperties, @Nullable ProjectReactor reactor) {
    LoggerFactory.getLogger(BatchSettings.class).info("Load batch settings");

    if (reactor != null) {
      String branch = bootstrapSettings.property(CoreProperties.PROJECT_BRANCH_PROPERTY);
      String projectKey = reactor.getRoot().getKey();
      if (StringUtils.isNotBlank(branch)) {
        projectKey = String.format("%s:%s", projectKey, branch);
      }
      downloadSettings(client, projectKey);
    } else {
      downloadSettings(client, null);
    }

    // order is important -> bottom-up. The last one overrides all the others.
    addProperties(bootstrapProperties.properties());
    if (reactor != null) {
      addProperties(reactor.getRoot().getProperties());
    }
    addEnvironmentVariables();
    addSystemProperties();
  }

  private void downloadSettings(ServerClient client, @Nullable String projectKey) {
    String url;
    if (StringUtils.isNotBlank(projectKey)) {
      url = "/batch_bootstrap/properties?project=" + projectKey;
    } else {
      url = "/batch_bootstrap/properties";
    }
    String jsonText = client.request(url);
    List<Map<String, String>> json = (List<Map<String, String>>) JSONValue.parse(jsonText);
    for (Map<String, String> jsonProperty : json) {
      String key = jsonProperty.get("k");
      String value = jsonProperty.get("v");
      String moduleKey = jsonProperty.get("p");
      if (moduleKey == null || projectKey == null || moduleKey.equals(projectKey)) {
        setProperty(key, value);
      }
      if (moduleKey != null) {
        Map<String, String> map = moduleProperties.get(moduleKey);
        if (map == null) {
          map = Maps.newHashMap();
          moduleProperties.put(moduleKey, map);
        }
        map.put(key, value);
      }
    }
  }

  public Map<String, String> getModuleProperties(String projectKey) {
    return moduleProperties.get(projectKey);
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
