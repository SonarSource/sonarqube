/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
import org.mockito.ArgumentCaptor;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProjectCleanerTest {

  public static final String DUMMY_PROFILE_CONTENT = "DUMMY PROFILE CONTENT";
  private static final String ROOT_UUID = "root";
  private static final String PROJECT_UUID = "project";
  private static final String BRANCH_UUID = "branch-uuid";
  private static final String ISSUE_PROJECT_UUID = "project-uuid";
  private ProjectCleaner underTest;
  private final PurgeDao dao = mock(PurgeDao.class);
  private final PurgeProfiler profiler = mock(PurgeProfiler.class);
  private final DefaultPeriodCleaner periodCleaner = mock(DefaultPeriodCleaner.class);
  private final PurgeListener purgeListener = mock(PurgeListener.class);
  private final MapSettings settings = new MapSettings(new PropertyDefinitions(System2.INSTANCE, PurgeProperties.all()));
  private final TelemetryQGOnMergedPRDataLoader telemetryQGOnMergedPRDataLoader = mock(TelemetryQGOnMergedPRDataLoader.class);

  @Rule
  public LogTester logTester = new LogTester();

  @Before
  public void before() {
    this.underTest = new ProjectCleaner(dao, periodCleaner, profiler, new PurgeListener[]{purgeListener}, telemetryQGOnMergedPRDataLoader);
  }

  @Test
  public void call_period_cleaner_index_client_and_purge_dao() {
    settings.setProperty(PurgeConstants.DAYS_BEFORE_DELETING_CLOSED_ISSUES, 5);

    underTest.purge(mock(DbSession.class), ROOT_UUID, PROJECT_UUID, settings.asConfig(), emptySet());

    verify(periodCleaner).clean(any(), any(), any());
    verify(dao).purge(any(), any(), any(), any());
  }

  @Test
  public void no_profiling_when_property_is_false() {
    settings.setProperty(CoreProperties.PROFILING_LOG_PROPERTY, false);

    underTest.purge(mock(DbSession.class), ROOT_UUID, PROJECT_UUID, settings.asConfig(), emptySet());

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

    underTest.purge(mock(DbSession.class), ROOT_UUID, PROJECT_UUID, settings.asConfig(), emptySet());

    verify(profiler).getProfilingResult(anyLong());
    assertThat(logTester.getLogs(Level.INFO).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.joining()))
      .contains("Profiling for purge")
      .contains(DUMMY_PROFILE_CONTENT);
  }

  @Test
  public void composite_listener_dispatches_onBranchDeleted_to_all_listeners() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);

    PurgeListener composite = captureCompositeListener(listener1, listener2);
    composite.onBranchDeleted(BRANCH_UUID);

    verify(listener1).onBranchDeleted(BRANCH_UUID);
    verify(listener2).onBranchDeleted(BRANCH_UUID);
  }

  @Test
  public void composite_listener_continues_onBranchDeleted_when_one_listener_throws() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);
    doThrow(new RuntimeException("simulated failure")).when(listener1).onBranchDeleted(any());

    PurgeListener composite = captureCompositeListener(listener1, listener2);

    assertThatThrownBy(() -> composite.onBranchDeleted(BRANCH_UUID))
      .isInstanceOf(RuntimeException.class);
    verify(listener2).onBranchDeleted(BRANCH_UUID);
    assertThat(logTester.getLogs(Level.WARN).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.joining()))
      .contains("onBranchDeleted");
  }

  @Test
  public void composite_listener_rethrows_first_exception_onBranchDeleted_when_multiple_listeners_throw() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);
    RuntimeException firstEx = new RuntimeException("first failure");
    doThrow(firstEx).when(listener1).onBranchDeleted(any());
    doThrow(new RuntimeException("second failure")).when(listener2).onBranchDeleted(any());

    PurgeListener composite = captureCompositeListener(listener1, listener2);

    assertThatThrownBy(() -> composite.onBranchDeleted(BRANCH_UUID))
      .isSameAs(firstEx);
    verify(listener1).onBranchDeleted(BRANCH_UUID);
    verify(listener2).onBranchDeleted(BRANCH_UUID);
  }

  @Test
  public void composite_listener_dispatches_onIssuesRemoval_to_all_listeners() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);
    List<String> issueKeys = List.of("issue-1", "issue-2");

    PurgeListener composite = captureCompositeListener(listener1, listener2);
    composite.onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);

    verify(listener1).onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);
    verify(listener2).onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);
  }

  @Test
  public void composite_listener_continues_onIssuesRemoval_when_one_listener_throws() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);
    doThrow(new RuntimeException("simulated failure")).when(listener1).onIssuesRemoval(any(), any());
    List<String> issueKeys = List.of("issue-1");

    PurgeListener composite = captureCompositeListener(listener1, listener2);

    assertThatThrownBy(() -> composite.onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys))
      .isInstanceOf(RuntimeException.class);
    verify(listener2).onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);
    assertThat(logTester.getLogs(Level.WARN).stream().map(LogAndArguments::getFormattedMsg).collect(Collectors.joining()))
      .contains("onIssuesRemoval");
  }

  @Test
  public void composite_listener_rethrows_first_exception_onIssuesRemoval_when_multiple_listeners_throw() {
    PurgeListener listener1 = mock(PurgeListener.class);
    PurgeListener listener2 = mock(PurgeListener.class);
    RuntimeException firstEx = new RuntimeException("first failure");
    doThrow(firstEx).when(listener1).onIssuesRemoval(any(), any());
    doThrow(new RuntimeException("second failure")).when(listener2).onIssuesRemoval(any(), any());
    List<String> issueKeys = List.of("issue-1");

    PurgeListener composite = captureCompositeListener(listener1, listener2);

    assertThatThrownBy(() -> composite.onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys))
      .isSameAs(firstEx);
    verify(listener1).onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);
    verify(listener2).onIssuesRemoval(ISSUE_PROJECT_UUID, issueKeys);
  }

  private PurgeListener captureCompositeListener(PurgeListener... listeners) {
    ProjectCleaner cleaner = new ProjectCleaner(dao, periodCleaner, profiler, listeners, telemetryQGOnMergedPRDataLoader);
    cleaner.purge(mock(DbSession.class), ROOT_UUID, PROJECT_UUID, settings.asConfig(), emptySet());
    ArgumentCaptor<PurgeListener> listenerCaptor = ArgumentCaptor.forClass(PurgeListener.class);
    verify(dao).purge(any(), any(), listenerCaptor.capture(), any());
    return listenerCaptor.getValue();
  }
}
