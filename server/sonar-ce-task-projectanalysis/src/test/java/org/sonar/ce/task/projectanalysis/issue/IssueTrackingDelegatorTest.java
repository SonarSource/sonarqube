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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.component.BranchType;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class IssueTrackingDelegatorTest {
  @Mock
  private PullRequestTrackerExecution prBranchTracker;
  @Mock
  private ReferenceBranchTrackerExecution mergeBranchTracker;
  @Mock
  private TrackerExecution tracker;
  @Mock
  private AnalysisMetadataHolder analysisMetadataHolder;
  @Mock
  private Component component;
  @Mock
  private Tracking<DefaultIssue, DefaultIssue> trackingResult;

  private IssueTrackingDelegator underTest;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    underTest = new IssueTrackingDelegator(prBranchTracker, mergeBranchTracker, tracker, analysisMetadataHolder);
    when(tracker.track(component)).thenReturn(trackingResult);
    when(mergeBranchTracker.track(component)).thenReturn(trackingResult);
    when(prBranchTracker.track(component)).thenReturn(trackingResult);
  }

  @Test
  public void delegate_regular_tracker() {
    when(analysisMetadataHolder.getBranch()).thenReturn(mock(Branch.class));

    underTest.track(component);

    verify(tracker).track(component);
    verifyZeroInteractions(prBranchTracker);
    verifyZeroInteractions(mergeBranchTracker);
  }

  @Test
  public void delegate_merge_tracker() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.BRANCH);
    when(branch.isMain()).thenReturn(false);
    when(analysisMetadataHolder.getBranch()).thenReturn(branch);
    when(analysisMetadataHolder.isFirstAnalysis()).thenReturn(true);

    underTest.track(component);

    verify(mergeBranchTracker).track(component);
    verifyZeroInteractions(tracker);
    verifyZeroInteractions(prBranchTracker);

  }

  @Test
  public void delegate_pull_request_tracker() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.PULL_REQUEST);
    when(analysisMetadataHolder.getBranch()).thenReturn(mock(Branch.class));
    when(analysisMetadataHolder.isPullRequest()).thenReturn(true);

    underTest.track(component);

    verify(prBranchTracker).track(component);
    verifyZeroInteractions(tracker);
    verifyZeroInteractions(mergeBranchTracker);
  }
}
