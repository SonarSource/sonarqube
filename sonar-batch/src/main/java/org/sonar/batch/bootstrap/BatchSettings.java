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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
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
  public static final String BATCH_BOOTSTRAP_PROPERTIES_URL = "/batch_bootstrap/properties";
  private Configuration deprecatedConfiguration;
  private boolean preview;

  private final BootstrapSettings bootstrapSettings;
  private final ServerClient client;
  private final AnalysisMode mode;
  private Map<String, String> savedProperties;

  public BatchSettings(BootstrapSettings bootstrapSettings, PropertyDefinitions propertyDefinitions,
                       ServerClient client, Configuration deprecatedConfiguration, AnalysisMode mode) {

    super(propertyDefinitions);
    this.mode = mode;
    getEncryption().setPathToSecretKey(bootstrapSettings.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    this.bootstrapSettings = bootstrapSettings;
    this.client = client;
    this.deprecatedConfiguration = deprecatedConfiguration;
    init(null);
  }

  public void init(@Nullable ProjectReactor reactor) {
    savedProperties = this.getProperties();

    this.preview = mode.isPreview();
    if (reactor != null) {
      LoggerFactory.getLogger(BatchSettings.class).info("Load project settings");
      String branch = bootstrapSettings.property(CoreProperties.PROJECT_BRANCH_PROPERTY);
      String projectKey = reactor.getRoot().getKey();
      if (StringUtils.isNotBlank(branch)) {
        projectKey = String.format("%s:%s", projectKey, branch);
      }
      downloadSettings(projectKey);
    } else {
      LoggerFactory.getLogger(BatchSettings.class).info("Load batch settings");
      downloadSettings(null);
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

  private void downloadSettings(@Nullable String projectKey) {
    String url;
    if (StringUtils.isNotBlank(projectKey)) {
      url = BATCH_BOOTSTRAP_PROPERTIES_URL + "?project=" + projectKey + "&dryRun=" + preview;
    } else {
      url = BATCH_BOOTSTRAP_PROPERTIES_URL + "?dryRun=" + preview;
    }
    String jsonText = client.request(url);

    List<Map<String, String>> json = new Gson().fromJson(jsonText, new TypeToken<List<Map<String, String>>>() {
    }.getType());

    for (Map<String, String> jsonProperty : json) {
      String key = jsonProperty.get("k");
      String value = jsonProperty.get("v");
      setProperty(key, value);
    }
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
    if (preview && key.endsWith(".secured") && !key.contains(".license")) {
      throw new SonarException("Access to the secured property '" + key
        + "' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    }
  }
}
