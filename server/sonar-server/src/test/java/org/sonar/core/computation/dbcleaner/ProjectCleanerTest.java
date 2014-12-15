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

package org.sonar.core.computation.dbcleaner;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.*;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.properties.ProjectSettingsFactory;
import org.sonar.server.search.IndexClient;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

public class ProjectCleanerTest {

  private ProjectCleaner sut;
  private PurgeDao dao;
  private PurgeProfiler profiler;
  private DefaultPeriodCleaner periodCleaner;
  private PurgeListener purgeListener;
  private ProjectSettingsFactory projectSettingsFactory;
  private IndexClient indexClient;
  private IssueIndex issueIndex;
  private Settings settings;

  @Before
  public void before() throws Exception {
    this.dao = mock(PurgeDao.class);
    this.profiler = mock(PurgeProfiler.class);
    this.periodCleaner = mock(DefaultPeriodCleaner.class);
    this.purgeListener = mock(PurgeListener.class);
    this.settings = mock(Settings.class);
    this.projectSettingsFactory = mock(ProjectSettingsFactory.class);
    when(projectSettingsFactory.newProjectSettings(any(DbSession.class), any(Long.class))).thenReturn(settings);

    this.issueIndex = mock(IssueIndex.class);
    this.indexClient = mock(IndexClient.class);
    when(indexClient.get(IssueIndex.class)).thenReturn(issueIndex);

    this.sut = new ProjectCleaner(dao, periodCleaner, profiler, purgeListener, projectSettingsFactory, indexClient);
  }

  @Test
  public void no_profiling_when_property_is_false() throws Exception {
    when(settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)).thenReturn(false);
    when(projectSettingsFactory.newProjectSettings(any(DbSession.class), any(Long.class))).thenReturn(settings);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(profiler, never()).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void no_indexing_when_no_issue_to_delete() throws Exception {
    when(projectSettingsFactory.newProjectSettings(any(DbSession.class), any(Long.class))).thenReturn(settings);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(indexClient, never()).get(IssueIndex.class);
  }

  @Test
  public void profiling_when_property_is_true() throws Exception {
    when(settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)).thenReturn(true);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(profiler).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void call_period_cleaner_index_client_and_purge_dao() throws Exception {
    when(settings.getInt(DbCleanerConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES)).thenReturn(5);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(periodCleaner).clean(any(DbSession.class), any(Long.class), any(Settings.class));
    verify(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));
    verify(issueIndex).deleteClosedIssuesOfProjectBefore(any(String.class), any(Date.class));
  }

  @Test
  public void if_dao_purge_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));
  }

  @Test
  public void if_profiler_cleaning_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(periodCleaner).clean(any(DbSession.class), anyLong(), any(Settings.class));

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class));

    verify(periodCleaner).clean(any(DbSession.class), anyLong(), any(Settings.class));
  }
}
