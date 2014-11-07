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

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.config.Settings;
import org.sonar.core.component.ComponentDto;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.dbcleaner.ProjectPurgeTask;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.properties.ProjectSettings;
import org.sonar.server.properties.ProjectSettingsFactory;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DataCleanerStepTest {

  private DataCleanerStep sut;
  private ProjectPurgeTask purgeTask;
  private IssueIndex issueIndex;
  private Settings settings;
  private ProjectSettingsFactory projectSettingsFactory;

  @Before
  public void before() {
    this.purgeTask = mock(ProjectPurgeTask.class);
    this.issueIndex = mock(IssueIndex.class);
    this.settings = mock(ProjectSettings.class);
    this.projectSettingsFactory = mock(ProjectSettingsFactory.class);
    when(projectSettingsFactory.newProjectSettings(anyLong(), any(DbSession.class))).thenReturn(settings);

    this.sut = new DataCleanerStep(projectSettingsFactory, purgeTask, issueIndex);
  }

  @Test
  public void call_purge_method_of_the_purge_task() {
    AnalysisReportDto report = mock(AnalysisReportDto.class);
    ComponentDto project = mock(ComponentDto.class);

    sut.execute(mock(DbSession.class), report, project);

    verify(projectSettingsFactory).newProjectSettings(anyLong(), any(DbSession.class));
    verify(purgeTask).purge(any(PurgeConfiguration.class), any(Settings.class), any(DbSession.class));
    verify(issueIndex).deleteClosedIssuesOfProjectBefore(anyString(), any(Date.class));
  }
}
