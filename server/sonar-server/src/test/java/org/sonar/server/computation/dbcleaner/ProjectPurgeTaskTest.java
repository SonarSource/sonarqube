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

package org.sonar.server.computation.dbcleaner;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.Settings;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.purge.PurgeConfiguration;
import org.sonar.core.purge.PurgeDao;
import org.sonar.core.purge.PurgeListener;
import org.sonar.core.purge.PurgeProfiler;
import org.sonar.server.computation.dbcleaner.period.DefaultPeriodCleaner;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;

public class ProjectPurgeTaskTest {

  private ProjectPurgeTask sut;
  private PurgeDao dao;
  private PurgeProfiler profiler;
  private DefaultPeriodCleaner periodCleaner;
  private PurgeListener purgeListener;

  @Before
  public void before() throws Exception {
    this.dao = mock(PurgeDao.class);
    this.profiler = mock(PurgeProfiler.class);
    this.periodCleaner = mock(DefaultPeriodCleaner.class);
    this.purgeListener = mock(PurgeListener.class);

    this.sut = new ProjectPurgeTask(dao, periodCleaner, profiler, purgeListener);
  }

  @Test
  public void no_profiling_when_property_is_false() throws Exception {
    Settings settings = mock(Settings.class);
    when(settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)).thenReturn(false);

    sut.purge(mock(DbSession.class), mock(PurgeConfiguration.class), settings);

    verify(profiler, never()).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void profiling_when_property_is_true() throws Exception {
    Settings settings = mock(Settings.class);
    when(settings.getBoolean(CoreProperties.PROFILING_LOG_PROPERTY)).thenReturn(true);

    sut.purge(mock(DbSession.class), mock(PurgeConfiguration.class), settings);

    verify(profiler, times(1)).dump(anyLong(), any(Logger.class));
  }

  @Test
  public void if_dao_purge_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(dao).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));

    sut.purge(mock(DbSession.class), mock(PurgeConfiguration.class), mock(Settings.class));

    verify(dao, times(1)).purge(any(DbSession.class), any(PurgeConfiguration.class), any(PurgeListener.class));
  }

  @Test
  public void if_profiler_cleaning_fails_it_should_not_interrupt_program_execution() throws Exception {
    doThrow(RuntimeException.class).when(periodCleaner).clean(any(DbSession.class), anyLong(), any(Settings.class));

    sut.purge(mock(DbSession.class), mock(PurgeConfiguration.class), mock(Settings.class));

    verify(periodCleaner, times(1)).clean(any(DbSession.class), anyLong(), any(Settings.class));
  }
}
