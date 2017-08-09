/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sonar.db.component.BranchType;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class IssueTrackingDelegatorTest {
  @Mock
  private ShortBranchTrackerExecution shortBranchTracker;
  @Mock
  private TrackerExecution tracker;
  @Mock
  private AnalysisMetadataHolder analysisMetadataHolder;
  @Mock
  private Component component;

  private IssueTrackingDelegator underTest;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    underTest = new IssueTrackingDelegator(shortBranchTracker, tracker, analysisMetadataHolder);
  }

  @Test
  public void delegate_regular_tracker() {
    when(analysisMetadataHolder.getBranch()).thenReturn(Optional.empty());

    underTest.track(component);

    verify(tracker).track(component);
    verifyZeroInteractions(shortBranchTracker);
  }

  @Test
  public void delegate_short_branch_tracker() {
    Branch branch = mock(Branch.class);
    when(branch.getType()).thenReturn(BranchType.SHORT);
    when(analysisMetadataHolder.getBranch()).thenReturn(Optional.of(branch));

    underTest.track(component);

    verify(shortBranchTracker).track(component);
    verifyZeroInteractions(tracker);

  }
}
