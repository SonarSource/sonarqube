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
import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.slf4j.LoggerFactory;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.resources.Project;
import org.sonar.batch.bootstrapper.ProjectDefinition;
import org.sonar.batch.bootstrapper.Reactor;

import java.io.IOException;
import java.util.*;

public class ProjectTree {

  private ProjectBuilder projectBuilder;
  private List<Project> projects;
  private List<ProjectDefinition> definitions;
  private Map<ProjectDefinition, Project> projectsMap;

  public ProjectTree(Reactor sonarReactor, DatabaseSession databaseSession) {
    this.projectBuilder = new ProjectBuilder(databaseSession);
    definitions = Lists.newArrayList();
    for (ProjectDefinition project : sonarReactor.getSortedProjects()) {
      collectProjects(project, definitions);
    }
  }

  /**
   * for unit tests
   */
  protected ProjectTree(ProjectBuilder projectBuilder, List<MavenProject> poms) {
    this.projectBuilder = projectBuilder;
    definitions = Lists.newArrayList();
    collectProjects(MavenProjectConverter.convert(poms, poms.get(0)), definitions);
  }

  /**
   * for unit tests
   */
  protected ProjectTree(List<Project> projects) {
    this.projects = new ArrayList<Project>(projects);
  }

  /**
   * Populates list of projects from hierarchy.
   */
  private static void collectProjects(ProjectDefinition root, List<ProjectDefinition> collected) {
    collected.add(root);
    for (ProjectDefinition module : root.getModules()) {
      collectProjects(module, collected);
    }
  }

  public void start() throws IOException {
    projects = Lists.newArrayList();
    projectsMap = Maps.newHashMap();

    for (ProjectDefinition def : definitions) {
      Project project = projectBuilder.create(def);
      projectsMap.put(def, project);
      projects.add(project);
    }

    for (Map.Entry<ProjectDefinition, Project> entry : projectsMap.entrySet()) {
      ProjectDefinition def = entry.getKey();
      Project project = entry.getValue();
      for (ProjectDefinition module : def.getModules()) {
        projectsMap.get(module).setParent(project);
      }
    }

    // Configure
    for (Map.Entry<ProjectDefinition, Project> entry : projectsMap.entrySet()) {
      projectBuilder.configure(entry.getValue(), entry.getKey());
    }

    applyModuleExclusions();
  }

  void applyModuleExclusions() {
    for (Project project : projects) {
      String[] excludedArtifactIds = project.getConfiguration().getStringArray("sonar.skippedModules");
      String[] includedArtifactIds = project.getConfiguration().getStringArray("sonar.includedModules");

      Set<String> includedModulesIdSet = new HashSet<String>();
      Set<String> excludedModulesIdSet = new HashSet<String>();

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

    for (Iterator<Project> it = projects.iterator(); it.hasNext();) {
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
    for (Map.Entry<ProjectDefinition, Project> entry : projectsMap.entrySet()) {
      if (ObjectUtils.equals(entry.getValue(), project)) {
        return entry.getKey();
      }
    }
    throw new IllegalStateException("Can not find ProjectDefinition for " + project);
  }
}
