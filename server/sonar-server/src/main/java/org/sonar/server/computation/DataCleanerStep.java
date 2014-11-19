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

package org.sonar.server.computation;

import org.sonar.api.config.Settings;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.dbcleaner.ProjectPurgeTask;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.properties.ProjectSettingsFactory;

import static org.sonar.core.purge.PurgeConfiguration.newDefaultPurgeConfiguration;

public class DataCleanerStep implements ComputationStep {
  private final ProjectPurgeTask purgeTask;
  private final IssueIndex issueIndex;
  private final ProjectSettingsFactory projectSettingsFactory;

  public DataCleanerStep(ProjectSettingsFactory projectSettingsFactory, ProjectPurgeTask purgeTask, IssueIndex issueIndex) {
    this.projectSettingsFactory = projectSettingsFactory;
    this.purgeTask = purgeTask;
    this.issueIndex = issueIndex;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report, ComponentDto project) {
    Long projectId = project.getId();

    Settings settings = projectSettingsFactory.newProjectSettings(session, projectId);
    PurgeConfiguration purgeConfiguration = newDefaultPurgeConfiguration(settings, projectId);

    purgeTask.purge(session, purgeConfiguration, settings);

    if (purgeConfiguration.maxLiveDateOfClosedIssues() != null) {
      issueIndex.deleteClosedIssuesOfProjectBefore(project.uuid(), purgeConfiguration.maxLiveDateOfClosedIssues());
    }
  }

  @Override
  public String getDescription() {
    return "Purge database";
  }
}
