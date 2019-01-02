/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.Test;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.util.stream.MoreCollectors.uniqueIndex;

public class TrackerExecutionTest {
  private final TrackerRawInputFactory rawInputFactory = mock(TrackerRawInputFactory.class);
  private final TrackerBaseInputFactory baseInputFactory = mock(TrackerBaseInputFactory.class);
  private final ClosedIssuesInputFactory closedIssuesInputFactory = mock(ClosedIssuesInputFactory.class);
  private final Tracker<DefaultIssue, DefaultIssue> tracker = mock(Tracker.class);
  private final ComponentIssuesLoader componentIssuesLoader = mock(ComponentIssuesLoader.class);
  private final AnalysisMetadataHolder analysisMetadataHolder = mock(AnalysisMetadataHolder.class);

  private TrackerExecution underTest = new TrackerExecution(baseInputFactory, rawInputFactory, closedIssuesInputFactory, tracker, componentIssuesLoader, analysisMetadataHolder);

  private Input<DefaultIssue> rawInput = mock(Input.class);
  private Input<DefaultIssue> openIssuesInput = mock(Input.class);
  private Input<DefaultIssue> closedIssuesInput = mock(Input.class);
  private NonClosedTracking<DefaultIssue, DefaultIssue> nonClosedTracking = mock(NonClosedTracking.class);
  private Tracking<DefaultIssue, DefaultIssue> closedTracking = mock(Tracking.class);

  @Test
  public void track_tracks_only_nonClosed_issues_if_tracking_returns_complete_from_Tracker() {
    ReportComponent component = ReportComponent.builder(Component.Type.FILE, 1).build();
    when(rawInputFactory.create(component)).thenReturn(rawInput);
    when(baseInputFactory.create(component)).thenReturn(openIssuesInput);
    when(closedIssuesInputFactory.create(any())).thenThrow(new IllegalStateException("closedIssuesInputFactory should not be called"));
    when(nonClosedTracking.isComplete()).thenReturn(true);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(tracker.trackNonClosed(rawInput, openIssuesInput)).thenReturn(nonClosedTracking);
    when(tracker.trackClosed(any(), any())).thenThrow(new IllegalStateException("trackClosed should not be called"));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(component);

    assertThat(tracking).isSameAs(nonClosedTracking);
    verify(tracker).trackNonClosed(rawInput, openIssuesInput);
    verifyNoMoreInteractions(tracker);
  }

  @Test
  public void track_does_not_track_nonClosed_issues_if_tracking_returns_incomplete_but_this_is_first_analysis() {
    ReportComponent component = ReportComponent.builder(Component.Type.FILE, 1).build();
    when(rawInputFactory.create(component)).thenReturn(rawInput);
    when(baseInputFactory.create(component)).thenReturn(openIssuesInput);
    when(closedIssuesInputFactory.create(any())).thenThrow(new IllegalStateException("closedIssuesInputFactory should not be called"));
    when(nonClosedTracking.isComplete()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);
    when(tracker.trackNonClosed(rawInput, openIssuesInput)).thenReturn(nonClosedTracking);
    when(tracker.trackClosed(any(), any())).thenThrow(new IllegalStateException("trackClosed should not be called"));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(component);

    assertThat(tracking).isSameAs(nonClosedTracking);
    verify(tracker).trackNonClosed(rawInput, openIssuesInput);
    verifyNoMoreInteractions(tracker);
  }

  @Test
  public void track_tracks_nonClosed_issues_and_then_closedOnes_if_tracking_returns_incomplete() {
    ReportComponent component = ReportComponent.builder(Component.Type.FILE, 1).build();
    when(rawInputFactory.create(component)).thenReturn(rawInput);
    when(baseInputFactory.create(component)).thenReturn(openIssuesInput);
    when(closedIssuesInputFactory.create(component)).thenReturn(closedIssuesInput);
    when(nonClosedTracking.isComplete()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(tracker.trackNonClosed(rawInput, openIssuesInput)).thenReturn(nonClosedTracking);
    when(tracker.trackClosed(nonClosedTracking, closedIssuesInput)).thenReturn(closedTracking);

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(component);

    assertThat(tracking).isSameAs(closedTracking);
    verify(tracker).trackNonClosed(rawInput, openIssuesInput);
    verify(tracker).trackClosed(nonClosedTracking, closedIssuesInput);
    verifyNoMoreInteractions(tracker);
  }

  @Test
  public void track_loadChanges_on_matched_closed_issues() {
    ReportComponent component = ReportComponent.builder(Component.Type.FILE, 1).build();
    when(rawInputFactory.create(component)).thenReturn(rawInput);
    when(baseInputFactory.create(component)).thenReturn(openIssuesInput);
    when(closedIssuesInputFactory.create(component)).thenReturn(closedIssuesInput);
    when(nonClosedTracking.isComplete()).thenReturn(false);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(false);
    when(tracker.trackNonClosed(rawInput, openIssuesInput)).thenReturn(nonClosedTracking);
    when(tracker.trackClosed(nonClosedTracking, closedIssuesInput)).thenReturn(closedTracking);
    Set<DefaultIssue> mappedClosedIssues = IntStream.range(1, 2 + new Random().nextInt(2))
      .mapToObj(i -> new DefaultIssue().setKey("closed" + i).setStatus(Issue.STATUS_CLOSED))
      .collect(toSet());

    ArrayList<DefaultIssue> mappedBaseIssues = new ArrayList<>(mappedClosedIssues);
    Issue.STATUSES.stream().filter(t -> !Issue.STATUS_CLOSED.equals(t)).forEach(s -> mappedBaseIssues.add(new DefaultIssue().setKey(s).setStatus(s)));
    Collections.shuffle(mappedBaseIssues);
    when(closedTracking.getMatchedRaws()).thenReturn(mappedBaseIssues.stream().collect(uniqueIndex(i -> new DefaultIssue().setKey("raw_for_" + i.key()), i -> i)));

    Tracking<DefaultIssue, DefaultIssue> tracking = underTest.track(component);

    assertThat(tracking).isSameAs(closedTracking);
    verify(tracker).trackNonClosed(rawInput, openIssuesInput);
    verify(tracker).trackClosed(nonClosedTracking, closedIssuesInput);
    verify(componentIssuesLoader).loadLatestDiffChangesForReopeningOfClosedIssues(mappedClosedIssues);
    verifyNoMoreInteractions(tracker);
  }
}
