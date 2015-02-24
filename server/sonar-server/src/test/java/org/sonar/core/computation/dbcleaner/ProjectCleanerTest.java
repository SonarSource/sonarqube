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
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.api.utils.log.Logger;
import org.sonar.core.computation.dbcleaner.period.DefaultPeriodCleaner;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.IdUuidPair;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeListener;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.properties.ProjectSettingsFactory;

import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

public class ProjectCleanerTest {

  private ProjectCleaner sut;
  private PurgeDao dao= mock(PurgeDao.class);
  private PurgeProfiler profiler= mock(PurgeProfiler.class);
  private DefaultPeriodCleaner periodCleaner= mock(DefaultPeriodCleaner.class);
  private PurgeListener purgeListener= mock(PurgeListener.class);
  private ProjectSettingsFactory projectSettingsFactory;
  private IssueIndex issueIndex= mock(IssueIndex.class);
  private Settings settings = new Settings();

  @Before
  public void before() throws Exception {
    this.projectSettingsFactory = mock(ProjectSettingsFactory.class);

    this.sut = new ProjectCleaner(dao, periodCleaner, profiler, purgeListener, issueIndex);
  }

  @Test
  public void no_profiling_when_property_is_false() throws Exception {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, false);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verify(profiler, never()).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void no_indexing_when_no_issue_to_delete() throws Exception {
    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verifyZeroInteractions(issueIndex);
  }

  @Test
  public void profiling_when_property_is_true() throws Exception {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, true);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verify(profiler).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void call_period_cleaner_index_client_and_purge_dao() throws Exception {
    settings.setProperty(DbCleanerConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verify(periodCleaner).clean(any(DbSession.class), any(Long.class), any(Settings.class));
    verify(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));
    verify(issueIndex).deleteClosedIssuesOfProjectBefore(any(String.class), any(Date.class));
  }

  @Test
  public void if_dao_purge_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verify(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));
  }

  @Test
  public void if_profiler_cleaning_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(periodCleaner).clean(any(DbSession.class), anyLong(), any(Settings.class));

    sut.purge(mock(DbSession.class), mock(IdUuidPair.class), settings);

    verify(periodCleaner).clean(any(DbSession.class), anyLong(), any(Settings.class));
  }
}
