/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.MessageException;
import org.sonar.batch.settings.SettingsReferential;

import javax.annotation.Nullable;

import java.util.Map;

public class BatchSettings extends Settings {

  private static final Logger LOG = LoggerFactory.getLogger(BatchSettings.class);

  private Configuration deprecatedConfiguration;

  private final BootstrapProperties bootstrapProps;
  private final SettingsReferential settingsReferential;
  private final AnalysisMode mode;
  private Map<String, String> savedProperties;

  public BatchSettings(BootstrapProperties bootstrapProps, PropertyDefinitions propertyDefinitions,
    SettingsReferential settingsReferential, Configuration deprecatedConfiguration, AnalysisMode mode) {

    super(propertyDefinitions);
    this.mode = mode;
    getEncryption().setPathToSecretKey(bootstrapProps.property(CoreProperties.ENCRYPTION_SECRET_KEY_PATH));
    this.bootstrapProps = bootstrapProps;
    this.settingsReferential = settingsReferential;
    this.deprecatedConfiguration = deprecatedConfiguration;
    init(null);
  }

  public void init(@Nullable ProjectReactor reactor) {
    savedProperties = this.getProperties();

    if (reactor != null) {
      LOG.info("Load project settings");

      String branch = reactor.getRoot().getProperties().getProperty(CoreProperties.PROJECT_BRANCH_PROPERTY);
      String projectKey = reactor.getRoot().getKey();
      if (StringUtils.isNotBlank(branch)) {
        projectKey = String.format("%s:%s", projectKey, branch);
      }
      downloadSettings(projectKey);
    } else {
      LOG.info("Load global settings");
      downloadSettings(null);
    }

    addProperties(bootstrapProps.properties());
    if (reactor != null) {
      addProperties(reactor.getRoot().getProperties());
    }
  }

  /**
   * Restore properties like they were before call of the {@link #init(org.sonar.api.batch.bootstrap.ProjectReactor)} method
   */
  public void restore() {
    this.setProperties(savedProperties);
  }

  private void downloadSettings(@Nullable String projectKey) {
    if (StringUtils.isNotBlank(projectKey)) {
      addProperties(settingsReferential.projectSettings(projectKey));
    } else {
      addProperties(settingsReferential.globalSettings());
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
    if (mode.isPreview() && key.endsWith(".secured") && !key.contains(".license")) {
      throw MessageException.of("Access to the secured property '" + key
        + "' is not possible in preview mode. The SonarQube plugin which requires this property must be deactivated in preview mode.");
    }
  }
}
