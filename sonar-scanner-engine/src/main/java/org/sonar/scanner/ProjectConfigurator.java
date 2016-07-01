/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.scanner;

import java.util.Date;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.CoreProperties;
import org.sonar.api.batch.ScannerSide;
import org.sonar.api.batch.bootstrap.ProjectDefinition;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.System2;

/**
 * Used by views !!
 *
 */
@ScannerSide
public class ProjectConfigurator {

  private final System2 system2;
  private Settings settings;

  public ProjectConfigurator(Settings settings, System2 system2) {
    this.settings = settings;
    this.system2 = system2;
  }

  public Project create(ProjectDefinition definition) {
    Project project = new Project(definition.getKey(), definition.getBranch(), definition.getName());
    project.setDescription(StringUtils.defaultString(definition.getDescription()));
    project.setOriginalName(definition.getOriginalName());
    return project;
  }

  public ProjectConfigurator configure(Project project) {
    Date analysisDate = loadAnalysisDate();
    project
      .setAnalysisDate(analysisDate)
      .setAnalysisVersion(loadAnalysisVersion());
    return this;
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

  private String loadAnalysisVersion() {
    return settings.getString(CoreProperties.PROJECT_VERSION_PROPERTY);
  }
}
