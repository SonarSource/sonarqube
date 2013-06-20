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
import org.sonar.api.utils.SonarException;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class BatchSettings extends Settings {
  private Configuration deprecatedConfiguration;
  private boolean dryRun;

  // Keep module settings for initialization of ProjectSettings
  // module key -> <key,val>
  private Map<String, Map<String, String>> moduleProperties = Maps.newHashMap();

  private final BootstrapSettings bootstrapSettings;
  private final ServerClient client;
  private Map<String, String> savedProperties;

  public BatchSettings(BootstrapSettings bootstrapSettings, PropertyDefinitions propertyDefinitions,
      ServerClient client, Configuration deprecatedConfiguration) {
    super(propertyDefinitions);
    this.bootstrapSettings = bootstrapSettings;
    this.client = client;
    this.deprecatedConfiguration = deprecatedConfiguration;
    init(null);
  }

  public void init(@Nullable ProjectReactor reactor) {
    savedProperties = this.getProperties();

    // Do not use getBoolean to avoid recursion
    this.dryRun = "true".equals(bootstrapSettings.property(CoreProperties.DRY_RUN));
    if (reactor != null) {
      LoggerFactory.getLogger(BatchSettings.class).info("Load project settings");
      String branch = bootstrapSettings.property(CoreProperties.PROJECT_BRANCH_PROPERTY);
      String projectKey = reactor.getRoot().getKey();
      if (StringUtils.isNotBlank(branch)) {
        projectKey = String.format("%s:%s", projectKey, branch);
      }
      downloadSettings(client, projectKey);
    } else {
      LoggerFactory.getLogger(BatchSettings.class).info("Load batch settings");
      downloadSettings(client, null);
    }

    addProperties(bootstrapSettings.properties());
    if (reactor != null) {
      addProperties(reactor.getRoot().getProperties());
    }
    properties.putAll(System.getenv());
    addProperties(System.getProperties());
  }

  /**
   * Restore properties like they were before call of the {@link #init(org.sonar.api.batch.bootstrap.ProjectReactor)} method
   */
  public void restore() {
    this.setProperties(savedProperties);
  }

  private void downloadSettings(ServerClient client, @Nullable String projectKey) {
    String url;
    if (StringUtils.isNotBlank(projectKey)) {
      url = "/batch_bootstrap/properties?project=" + projectKey + "&dryRun=" + dryRun;
    } else {
      url = "/batch_bootstrap/properties?dryRun=" + dryRun;
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

  @Override
  protected void doOnGetProperties(String key) {
    if (dryRun && key.endsWith(".secured") && !key.contains(".license")) {
      throw new SonarException("Access to the secured property '" + key
        + "' is not possible in local (dry run) SonarQube analysis. The SonarQube plugin accessing to this property must be deactivated in dry run mode.");
    }
  }
}
