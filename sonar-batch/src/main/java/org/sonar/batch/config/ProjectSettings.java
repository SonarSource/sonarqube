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
package org.sonar.batch.config;

import com.google.common.collect.Lists;
import org.apache.commons.configuration.Configuration;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.Settings;
import org.sonar.api.database.configuration.Property;
import org.sonar.api.resources.Project;
import org.sonar.core.config.ConfigurationUtils;
import org.sonar.jpa.session.DatabaseSessionFactory;

import java.util.Iterator;
import java.util.List;

/**
 * @since 2.12
 */
public class ProjectSettings extends Settings {

  private Configuration deprecatedCommonsConf;
  private ProjectDefinition projectDefinition;
  private DatabaseSessionFactory dbFactory;

  public ProjectSettings(PropertyDefinitions definitions, ProjectDefinition projectDefinition, DatabaseSessionFactory dbFactory, Project project) {
    super(definitions);
    this.deprecatedCommonsConf = project.getConfiguration(); // Configuration is not a parameter to be sure that the project conf is used, not the global one
    this.projectDefinition = projectDefinition;
    this.dbFactory = dbFactory;
    load();
  }

  public ProjectSettings load() {
    clear();

    // order is important -> bottom-up. The last one overrides all the others.
    loadDatabaseGlobalSettings();
    loadDatabaseProjectSettings(projectDefinition);
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

  private void loadDatabaseProjectSettings(ProjectDefinition projectDef) {
    if (projectDef.getParent() != null) {
      loadDatabaseProjectSettings(projectDef.getParent());
    }
    List<Property> props = ConfigurationUtils.getProjectProperties(dbFactory, projectDef.getKey());
    for (Property dbProperty : props) {
      setProperty(dbProperty.getKey(), dbProperty.getValue());
    }
  }

  private void loadDatabaseGlobalSettings() {
    List<Property> props = ConfigurationUtils.getGeneralProperties(dbFactory);
    for (Property dbProperty : props) {
      setProperty(dbProperty.getKey(), dbProperty.getValue());
    }
  }

  private void updateDeprecatedCommonsConfiguration() {
    System.out.println("---------- SETTINGS -------------");
    for (String s : properties.keySet()) {
      System.out.println(s + "=" + properties.get(s));
    }
    ConfigurationUtils.copyToCommonsConfiguration(properties, deprecatedCommonsConf);

    System.out.println("---------- DEP CONF -------------");
    Iterator keys = deprecatedCommonsConf.getKeys();
    while(keys.hasNext()) {
      String key = (String)keys.next();
      System.out.println(key + "=" + deprecatedCommonsConf.getString(key));
    }
    System.out.println("----------------------------------");
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
