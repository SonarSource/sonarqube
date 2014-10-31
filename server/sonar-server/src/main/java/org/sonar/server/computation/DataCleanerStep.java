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
import org.sonar.core.computation.dbcleaner.DefaultPurgeTask;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.server.issue.index.IssueIndex;

import java.util.Date;

public class DataCleanerStep implements ComputationStep {
  private final DefaultPurgeTask purgeTask;
  private final IssueIndex issueIndex;
  private final Settings settings;

  public DataCleanerStep(DefaultPurgeTask purgeTask, IssueIndex issueIndex, Settings settings) {
    this.purgeTask = purgeTask;
    this.issueIndex = issueIndex;
    this.settings = settings;
  }

  @Override
  public void execute(DbSession session, AnalysisReportDto report, ComponentDto project) {
    Long projectId = project.getId();
    purgeTask.purge(projectId);
    issueIndex.deleteClosedIssuesOfProjectBefore(project.uuid(), dateBeforeWhichDeleteClosedIssues(projectId));
  }

  private Date dateBeforeWhichDeleteClosedIssues(Long resourceId) {
    return PurgeConfiguration.newDefaultPurgeConfiguration(resourceId, settings).maxLiveDateOfClosedIssues();
  }

  @Override
  public String getDescription() {
    return "Purge database";
  }
}
