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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.maven.project.MavenProject;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Java;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ProjectConfigurator {

  private DatabaseSession databaseSession;

  public ProjectConfigurator(DatabaseSession databaseSession) {
    this.databaseSession = databaseSession;
  }

  public Project create(ProjectDefinition definition) {
    Configuration configuration = getStartupConfiguration(definition);
    Project project = new Project(definition.getKey(), loadProjectBranch(configuration), definition.getName())
        .setDescription(StringUtils.defaultString(definition.getDescription(), ""))
        .setPackaging("jar");
    // For backward compatibility we must set POM and actual packaging
    for (Object component : definition.getContainerExtensions()) {
      if (component instanceof MavenProject) {
        MavenProject pom = (MavenProject) component;
        project.setPom(pom);
        project.setPackaging(pom.getPackaging());
      }
    }
    return project;
  }

  Configuration getStartupConfiguration(ProjectDefinition project) {
    CompositeConfiguration configuration = new CompositeConfiguration();
    configuration.addConfiguration(new SystemConfiguration());
    configuration.addConfiguration(new EnvironmentConfiguration());
    configuration.addConfiguration(new MapConfiguration(project.getProperties()));
    return configuration;
  }

  String loadProjectBranch(Configuration configuration) {
    return configuration.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
  }

  public void configure(Project project, ProjectDefinition def) {
    ProjectConfiguration projectConfiguration = new ProjectConfiguration(databaseSession, project, def.getProperties());
    configure(project, projectConfiguration);
  }

  void configure(Project project, Configuration projectConfiguration) {
    Date analysisDate = loadAnalysisDate(projectConfiguration);
    project.setConfiguration(projectConfiguration)
        .setExclusionPatterns(loadExclusionPatterns(projectConfiguration))
        .setAnalysisDate(analysisDate)
        .setLatestAnalysis(isLatestAnalysis(project.getKey(), analysisDate))
        .setAnalysisVersion(loadAnalysisVersion(projectConfiguration))
        .setAnalysisType(loadAnalysisType(projectConfiguration))
        .setLanguageKey(loadLanguageKey(projectConfiguration));
  }

  static String[] loadExclusionPatterns(Configuration configuration) {
    String[] exclusionPatterns = configuration.getStringArray(CoreProperties.PROJECT_EXCLUSIONS_PROPERTY);
    if (exclusionPatterns == null) {
      exclusionPatterns = new String[0];
    }
    for (int i = 0; i < exclusionPatterns.length; i++) {
      exclusionPatterns[i] = StringUtils.trim(exclusionPatterns[i]);
    }
    return exclusionPatterns;
  }

  boolean isLatestAnalysis(String projectKey, Date analysisDate) {
    ResourceModel persistedProject = databaseSession.getSingleResult(ResourceModel.class, "key", projectKey, "enabled", true);
    if (persistedProject != null) {
      Snapshot lastSnapshot = databaseSession.getSingleResult(Snapshot.class, "resourceId", persistedProject.getId(), "last", true);
      return lastSnapshot == null || lastSnapshot.getCreatedAt().before(analysisDate);
    }
    return true;
  }

  Date loadAnalysisDate(Configuration configuration) {
    String formattedDate = configuration.getString(CoreProperties.PROJECT_DATE_PROPERTY);
    if (formattedDate == null) {
      return new Date();
    }

    DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
    try {
      // see SONAR-908 make sure that a time is defined for the date.
      Date date = DateUtils.setHours(format.parse(formattedDate), 0);
      return DateUtils.setMinutes(date, 1);

    } catch (ParseException e) {
      throw new SonarException("The property " + CoreProperties.PROJECT_DATE_PROPERTY
          + " does not respect the format yyyy-MM-dd (for example 2008-05-23) : " + formattedDate, e);
    }
  }

  Project.AnalysisType loadAnalysisType(Configuration configuration) {
    String value = configuration.getString(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY);
    if (value == null) {
      return (configuration.getBoolean("sonar.light", false) ? Project.AnalysisType.STATIC : Project.AnalysisType.DYNAMIC);
    }
    if ("true".equals(value)) {
      return Project.AnalysisType.DYNAMIC;
    }
    if ("reuseReports".equals(value)) {
      return Project.AnalysisType.REUSE_REPORTS;
    }
    return Project.AnalysisType.STATIC;
  }

  String loadAnalysisVersion(Configuration configuration) {
    return configuration.getString(CoreProperties.PROJECT_VERSION_PROPERTY);
  }

  String loadLanguageKey(Configuration configuration) {
    return configuration.getString(CoreProperties.PROJECT_LANGUAGE_PROPERTY, Java.KEY);
  }
}
