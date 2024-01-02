/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.purge;

import java.util.List;
import java.util.stream.Collectors;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.CoreProperties;
import org.sonar.api.config.PropertyDefinitions;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.config.PurgeConstants;
import org.sonar.core.config.PurgeProperties;
import org.sonar.db.DbSession;
import org.sonar.db.purge.PurgeDao;
import org.sonar.db.purge.PurgeListener;
import org.sonar.db.purge.PurgeProfiler;
import org.sonar.db.purge.period.DefaultPeriodCleaner;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectCleanerTest {

  public static final String DUMMY_PROFILE_CONTENT = "DUMMY PROFILE CONTENT";
  private ProjectCleaner underTest;
  private PurgeDao dao = mock(PurgeDao.class);
  private PurgeProfiler profiler = mock(PurgeProfiler.class);
  private DefaultPeriodCleaner periodCleaner = mock(DefaultPeriodCleaner.class);
  private PurgeListener purgeListener = mock(PurgeListener.class);
  private MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, PurgeProperties.all()));

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void before() {
    this.underTest = new ProjectCleaner(dao, periodCleaner, profiler, purgeListener);
  }

  @Test
  public void call_period_cleaner_index_client_and_purge_dao() {
    settings.setProperty(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);

    underTest.purge(mock(DbSession.class), "root", "project", settings.asConfig(), emptySet());

    verify(periodCleaner).clean(any(), any(), any());
    verify(dao).purge(any(), any(), any(), any());
  }

  @Test
  public void no_profiling_when_property_is_false() {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, false);

    underTest.purge(mock(DbSession.class), "root", "project", settings.asConfig(), emptySet());

    verify(profiler, never()).getProfilingResult(anyLong());
    assertThat(logTester.getLogs().stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.joining()))
      .doesNotContain("Profiling for purge");
  }

  @Test
  public void profiling_when_property_is_true() {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, true);
    when(profiler.getProfilingResult(anyLong())).thenReturn(List.of(DUMMY_PROFILE_CONTENT));

    underTest.purge(mock(DbSession.class), "root", "project", settings.asConfig(), emptySet());

    verify(profiler).getProfilingResult(anyLong());
    assertThat(logTester.getLogs(Level.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.joining()))
      .contains("Profiling for purge")
      .contains(DUMMY_PROFILE_CONTENT);
  }
}
