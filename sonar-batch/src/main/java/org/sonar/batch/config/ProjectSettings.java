/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
package org.sonar.batch.config;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.core.config.ConfigurationUtils;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import java.util.List;

/**
 * @since 2.12
 */
public class ProjectSettings extends Settings {

  private Configuration deprecatedCommonsConf;
  private ProjectDefinition projectDefinition;
  private PropertiesDao propertiesDao;

  public ProjectSettings(PropertyDefinitions definitions, ProjectDefinition projectDefinition, PropertiesDao propertiesDao, Project project) {
    super(definitions);
    this.deprecatedCommonsConf = project.getConfiguration(); // Configuration is not a parameter to be sure that the project conf is used, not the global one
    this.projectDefinition = projectDefinition;
    this.propertiesDao = propertiesDao;
    load();
  }

  public ProjectSettings load() {
    clear();

    // hack to obtain "sonar.branch" before loading settings from database
    loadBuildProperties();
    addEnvironmentVariables();
    addSystemProperties();
    String branch = getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
    clear();

    // order is important -> bottom-up. The last one overrides all the others.
    loadDatabaseGlobalSettings();
    loadDatabaseProjectSettings(projectDefinition, branch);
    loadBuildProperties();
    addEnvironmentVariables();
    addSystemProperties();

    updateDeprecatedCommonsConfiguration();

    return this;
  }

  private void loadBuildProperties() {
    List<ProjectDefinition> orderedProjects = getOrderedProjects(projectDefinition);
    for (ProjectDefinition p : orderedProjects) {
      addProperties(p.getProperties());
    }
  }

  private void loadDatabaseProjectSettings(ProjectDefinition projectDef, String branch) {
    if (projectDef.getParent() != null) {
      loadDatabaseProjectSettings(projectDef.getParent(), branch);
    }
    String projectKey = projectDef.getKey();
    if (StringUtils.isNotBlank(branch)) {
      projectKey = String.format("%s:%s", projectKey, branch);
    }
    List<PropertyDto> props = propertiesDao.selectProjectProperties(projectKey);
    for (PropertyDto dbProperty : props) {
      setProperty(dbProperty.getKey(), dbProperty.getValue());
    }
  }

  private void loadDatabaseGlobalSettings() {
    List<PropertyDto> props = propertiesDao.selectGlobalProperties();
    for (PropertyDto dbProperty : props) {
      setProperty(dbProperty.getKey(), dbProperty.getValue());
    }
  }

  private void updateDeprecatedCommonsConfiguration() {
    ConfigurationUtils.copyToCommonsConfiguration(properties, deprecatedCommonsConf);
  }

  /**
   * From root to module
   */
  static List<ProjectDefinition> getOrderedProjects(ProjectDefinition project) {
    List<ProjectDefinition> result = Lists.newArrayList();
    ProjectDefinition pd = project;
    while (pd != null) {
      result.add(0, pd);
      pd = pd.getParent();
    }
    return result;
  }
}
