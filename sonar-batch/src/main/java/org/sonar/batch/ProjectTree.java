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
import com.google.common.collect.Sets;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.bootstrap.ProjectBuilder;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.batch.bootstrap.ProjectReactor;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;

import java.io.IOException;
import java.util.*;

public class ProjectTree {

  private ProjectConfigurator configurator;
  private ProjectReactor projectReactor;

  private List<Project> projects;
  private Map<ProjectDefinition, Project> projectsByDef;
  private Settings settings;

  public ProjectTree(ProjectReactor projectReactor, //NOSONAR the unused parameter 'builders' is used for the startup order of components
                     ProjectConfigurator projectConfigurator,
                     Settings settings,
                     /* Must be executed after ProjectBuilders */ ProjectBuilder[] builders) {
    this(projectReactor, projectConfigurator, settings);
  }

  public ProjectTree(ProjectReactor projectReactor, //NOSONAR the unused parameter 'builders' is used for the startup order of components
                     ProjectConfigurator projectConfigurator,
                     Settings settings) {
    this.projectReactor = projectReactor;
    this.configurator = projectConfigurator;
    this.settings = settings;
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
    for (Project project : projects) {
      String[] excludedArtifactIds = settings.getStringArray("sonar.skippedModules");
      String[] includedArtifactIds = settings.getStringArray("sonar.includedModules");

      Set<String> includedModulesIdSet = Sets.newHashSet();
      Set<String> excludedModulesIdSet = Sets.newHashSet();

      if (includedArtifactIds != null) {
        includedModulesIdSet.addAll(Arrays.asList(includedArtifactIds));
      }

      if (excludedArtifactIds != null) {
        excludedModulesIdSet.addAll(Arrays.asList(excludedArtifactIds));
        includedModulesIdSet.removeAll(excludedModulesIdSet);
      }

      if (!includedModulesIdSet.isEmpty()) {
        for (Project currentProject : projects) {
          if (!includedModulesIdSet.contains(getArtifactId(currentProject))) {
            exclude(currentProject);
          }
        }
      } else {
        for (String excludedArtifactId : excludedModulesIdSet) {
          Project excludedProject = getProjectByArtifactId(excludedArtifactId);
          exclude(excludedProject);
        }
      }
    }

    for (Iterator<Project> it = projects.iterator(); it.hasNext(); ) {
      Project project = it.next();
      if (project.isExcluded()) {
        LoggerFactory.getLogger(getClass()).info("Module {} is excluded from analysis", project.getName());
        project.removeFromParent();
        it.remove();
      }
    }
  }

  private void exclude(Project project) {
    if (project != null) {
      project.setExcluded(true);
      for (Project module : project.getModules()) {
        exclude(module);
      }
    }
  }

  public List<Project> getProjects() {
    return projects;
  }

  private String getArtifactId(Project project) {
    String key = project.getKey();
    if (StringUtils.isNotBlank(project.getBranch())) {
      // remove branch part
      key = StringUtils.removeEnd(project.getKey(), ":" + project.getBranch());
    }
    return StringUtils.substringAfterLast(key, ":");
  }

  public Project getProjectByArtifactId(String artifactId) {
    for (Project project : projects) {
      // TODO see http://jira.codehaus.org/browse/SONAR-2324
      if (StringUtils.equals(getArtifactId(project), artifactId)) {
        return project;
      }
    }
    return null;
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
