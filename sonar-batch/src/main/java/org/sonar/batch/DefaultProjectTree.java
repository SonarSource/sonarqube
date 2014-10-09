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
package org.sonar.batch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.ProjectReactorReady;

import java.util.List;
import java.util.Map;

public class DefaultProjectTree implements ProjectTree {

  private final ProjectConfigurator configurator;
  private ProjectReactor projectReactor;

  private List<Project> projects;
  private Map<ProjectDefinition, Project> projectsByDef;

  public DefaultProjectTree(ProjectReactor projectReactor,
      ProjectConfigurator projectConfigurator,
      ProjectReactorReady reactorReady) {
    this.projectReactor = projectReactor;
    this.configurator = projectConfigurator;
  }

  public void start() {
    doStart(projectReactor.getProjects());
  }

  void doStart(List<ProjectDefinition> definitions) {
    projects = Lists.newArrayList();
    projectsByDef = Maps.newHashMap();

    for (ProjectDefinition def : definitions) {
      Project project = configurator.create(def);
      projectsByDef.put(def, project);
      projects.add(project);
    }

    for (Map.Entry<ProjectDefinition, Project> entry : projectsByDef.entrySet()) {
      ProjectDefinition def = entry.getKey();
      Project project = entry.getValue();
      for (ProjectDefinition module : def.getSubProjects()) {
        projectsByDef.get(module).setParent(project);
      }
    }

    // Configure
    for (Project project : projects) {
      configurator.configure(project);
    }
  }

  public List<Project> getProjects() {
    return projects;
  }

  @Override
  public Project getRootProject() {
    for (Project project : projects) {
      if (project.getParent() == null) {
        return project;
      }
    }
    throw new IllegalStateException("Can not find the root project from the list of Maven modules");
  }

  @Override
  public ProjectDefinition getProjectDefinition(Project project) {
    for (Map.Entry<ProjectDefinition, Project> entry : projectsByDef.entrySet()) {
      if (ObjectUtils.equals(entry.getValue(), project)) {
        return entry.getKey();
      }
    }
    throw new IllegalStateException("Can not find ProjectDefinition for " + project);
  }
}
