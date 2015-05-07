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

import org.apache.commons.lang.StringUtils;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchSide;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.System2;

import javax.annotation.Nullable;

import java.util.Date;

import static org.sonar.api.utils.DateUtils.formatDateTime;

/**
 * Used by views !!
 *
 */
@BatchSide
public class ProjectConfigurator {

  private static final Logger LOG = LoggerFactory.getLogger(ProjectConfigurator.class);
  private final System2 system2;
  private DatabaseSession databaseSession;
  private Settings settings;

  public ProjectConfigurator(@Nullable DatabaseSession databaseSession, Settings settings, System2 system2) {
    this.databaseSession = databaseSession;
    this.settings = settings;
    this.system2 = system2;
  }

  public ProjectConfigurator(Settings settings, System2 system2) {
    this(null, settings, system2);
  }

  public Project create(ProjectDefinition definition) {
    Project project = new Project(definition.getKey(), loadProjectBranch(), definition.getName());

    // For backward compatibility we must set POM and actual packaging
    project.setDescription(StringUtils.defaultString(definition.getDescription()));
    project.setPackaging("jar");

    for (Object component : definition.getContainerExtensions()) {
      if (component instanceof MavenProject) {
        MavenProject pom = (MavenProject) component;
        project.setPom(pom);
        project.setPackaging(pom.getPackaging());
      }
    }
    return project;
  }

  private String loadProjectBranch() {
    return settings.getString(CoreProperties.PROJECT_BRANCH_PROPERTY);
  }

  public ProjectConfigurator configure(Project project) {
    Date analysisDate = loadAnalysisDate();
    checkCurrentAnalysisIsTheLatestOne(project.getKey(), analysisDate);

    project
      .setAnalysisDate(analysisDate)
      .setAnalysisVersion(loadAnalysisVersion())
      .setAnalysisType(loadAnalysisType());
    return this;
  }

  private void checkCurrentAnalysisIsTheLatestOne(String projectKey, Date analysisDate) {
    if (databaseSession != null) {
      ResourceModel persistedProject = databaseSession.getSingleResult(ResourceModel.class, "key", projectKey, "enabled", true);
      if (persistedProject != null) {
        Snapshot lastSnapshot = databaseSession.getSingleResult(Snapshot.class, "resourceId", persistedProject.getId(), "last", true);
        boolean analysisBeforeLastSnapshot = lastSnapshot != null && analysisDate.getTime() <= lastSnapshot.getCreatedAtMs();
        if (analysisBeforeLastSnapshot) {
          throw new IllegalArgumentException(
            "'sonar.projectDate' property cannot be older than the date of the last known quality snapshot on this project. Value: '" +
              settings.getString(CoreProperties.PROJECT_DATE_PROPERTY) + "'. " +
              "Latest quality snapshot: '" + formatDateTime(lastSnapshot.getCreatedAt())
              + "'. This property may only be used to rebuild the past in a chronological order.");
        }
      }
    }
  }

  private Date loadAnalysisDate() {
    Date date;
    try {
      // sonar.projectDate may have been specified as a time
      date = settings.getDateTime(CoreProperties.PROJECT_DATE_PROPERTY);
    } catch (SonarException e) {
      // this is probably just a date
      date = settings.getDate(CoreProperties.PROJECT_DATE_PROPERTY);
    }
    if (date == null) {
      date = new Date(system2.now());
      settings.setProperty(CoreProperties.PROJECT_DATE_PROPERTY, date, true);
    }
    return date;
  }

  private Project.AnalysisType loadAnalysisType() {
    String value = settings.getString(CoreProperties.DYNAMIC_ANALYSIS_PROPERTY);
    if (value == null) {
      return Project.AnalysisType.DYNAMIC;
    }

    LOG.warn("'sonar.dynamicAnalysis' is deprecated since version 4.3 and should no longer be used.");
    if ("true".equals(value)) {
      return Project.AnalysisType.DYNAMIC;
    }
    if ("reuseReports".equals(value)) {
      return Project.AnalysisType.REUSE_REPORTS;
    }
    return Project.AnalysisType.STATIC;
  }

  private String loadAnalysisVersion() {
    return settings.getString(CoreProperties.PROJECT_VERSION_PROPERTY);
  }
}
