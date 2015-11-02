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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.ObjectUtils;
import org.picocontainer.Startable;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.resources.Project;
import org.sonar.batch.scan.ImmutableProjectReactor;

public class DefaultProjectTree implements Startable {

  private final ProjectConfigurator configurator;
  private final ImmutableProjectReactor projectReactor;

  private List<Project> projects;
  private Map<ProjectDefinition, Project> projectsByDef;

  public DefaultProjectTree(ImmutableProjectReactor projectReactor, ProjectConfigurator projectConfigurator) {
    this.projectReactor = projectReactor;
    this.configurator = projectConfigurator;
  }

  @Override
  public void start() {
    doStart(projectReactor.getProjects());
  }

  @Override
  public void stop() {
    // Nothing to do
  }

  void doStart(Collection<ProjectDefinition> definitions) {
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

  public Project getRootProject() {
    for (Project project : projects) {
      if (project.getParent() == null) {
        return project;
      }
    }
    throw new IllegalStateException("Can not find the root project from the list of Maven modules");
  }

  public ProjectDefinition getProjectDefinition(Project project) {
    for (Map.Entry<ProjectDefinition, Project> entry : projectsByDef.entrySet()) {
      if (ObjectUtils.equals(entry.getValue(), project)) {
        return entry.getKey();
      }
    }
    throw new IllegalStateException("Can not find ProjectDefinition for " + project);
  }
}
