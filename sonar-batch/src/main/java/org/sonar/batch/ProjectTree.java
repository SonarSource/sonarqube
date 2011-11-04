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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang.ObjectUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrap.ProjectFilter;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ProjectTree {

  private ProjectConfigurator configurator;
  private ProjectReactor projectReactor;

  private List<Project> projects;
  private Map<ProjectDefinition, Project> projectsByDef;
  private ProjectFilter projectFilter;

  public ProjectTree(ProjectReactor projectReactor, //NOSONAR the unused parameter 'builders' is used for the startup order of components
                     ProjectConfigurator projectConfigurator,
                     ProjectFilter projectFilter,
                     /* Must be executed after ProjectBuilders */ ProjectBuilder[] builders) {
    this(projectReactor, projectConfigurator, projectFilter);
  }

  public ProjectTree(ProjectReactor projectReactor, //NOSONAR the unused parameter 'builders' is used for the startup order of components
                     ProjectConfigurator projectConfigurator,
                     ProjectFilter projectFilter) {
    this.projectReactor = projectReactor;
    this.configurator = projectConfigurator;
    this.projectFilter = projectFilter;
  }

  ProjectTree(ProjectConfigurator configurator) {
    this.configurator = configurator;
  }

  public void start() throws IOException {
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

    applyExclusions();
  }

  void applyExclusions() {
    for (Iterator<Project> it = projects.iterator(); it.hasNext(); ) {
      Project project = it.next();
      if (projectFilter.isExcluded(project)) {
        project.setExcluded(true);
        LoggerFactory.getLogger(getClass()).info("Project {} excluded", project.getName());
        project.removeFromParent();
        it.remove();
      }
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
