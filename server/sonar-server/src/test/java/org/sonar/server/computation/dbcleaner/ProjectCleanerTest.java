/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.computation.dbcleaner;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.config.PurgeProperties;
import org.sonar.db.DbSession;
import org.sonar.db.purge.IdUuidPair;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeListener;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.period.DefaultPeriodCleaner;

import static java.util.Collections.emptyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class ProjectCleanerTest {

  private ProjectCleaner underTest;
  private PurgeDao dao = mock(PurgeDao.class);
  private PurgeProfiler profiler = mock(PurgeProfiler.class);
  private DefaultPeriodCleaner periodCleaner = mock(DefaultPeriodCleaner.class);
  private PurgeListener purgeListener = mock(PurgeListener.class);
  private MapSettings settings = new MapSettings(new PropertyDefinitions(PurgeProperties.all()));

  @Before
  public void before() {
    this.underTest = new ProjectCleaner(dao, periodCleaner, profiler, purgeListener);
  }

  @Test
  public void no_profiling_when_property_is_false() {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, false);

    underTest.purge(mock(DbSession.class), mock(IdUuidPair.class), settings.asConfig(), emptyList());

    verify(profiler, never()).dump(anyLong(), any());
  }

  @Test
  public void profiling_when_property_is_true() {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, true);

    underTest.purge(mock(DbSession.class), mock(IdUuidPair.class), settings.asConfig(), emptyList());

    verify(profiler).dump(anyLong(), any());
  }

  @Test
  public void call_period_cleaner_index_client_and_purge_dao() {
    settings.setProperty(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);

    underTest.purge(mock(DbSession.class), mock(IdUuidPair.class), settings.asConfig(), emptyList());

    verify(periodCleaner).clean(any(), any(), any());
    verify(dao).purge(any(), any(), any(), any());
  }
}
