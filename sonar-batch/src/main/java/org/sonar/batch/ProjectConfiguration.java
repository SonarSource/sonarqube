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
package org.sonar.batch;

import org.apache.commons.configuration.*;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;

import java.util.Properties;

public class ProjectConfiguration extends CompositeConfiguration {
  private PropertiesConfiguration runtimeConfiguration;

  /**
   * Used during batch startup
   */
  public ProjectConfiguration(DatabaseSession session, ProjectReactor projectReactor) {
    this(session, projectReactor.getRoot());
  }
  
  public ProjectConfiguration(DatabaseSession session, ProjectDefinition project) {
    runtimeConfiguration = new PropertiesConfiguration();
    addConfiguration(runtimeConfiguration);

    loadSystemSettings();
    loadProjectDatabaseSettings(session, project);
    addConfiguration(new MapConfiguration(project.getProperties()));
    loadGlobalDatabaseSettings(session);
  }

  private void loadProjectDatabaseSettings(DatabaseSession session, ProjectDefinition project) {
    addConfiguration(new ResourceDatabaseConfiguration(session, project.getKey()));

    ProjectDefinition parent = project.getParent();
    while (parent != null && parent.getKey() != null) {
      addConfiguration(new ResourceDatabaseConfiguration(session, parent.getKey()));
      parent = parent.getParent();
    }
  }

  private void loadGlobalDatabaseSettings(DatabaseSession session) {
    addConfiguration(new org.sonar.api.database.configuration.DatabaseConfiguration(session));
  }

  private void loadSystemSettings() {
    addConfiguration(new SystemConfiguration());
    addConfiguration(new EnvironmentConfiguration());
  }

  @Override
  public void setProperty(String s, Object o) {
    runtimeConfiguration.setProperty(s, o);
  }
}
