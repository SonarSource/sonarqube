/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import javax.annotation.Nullable;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracking;

import static java.util.Collections.emptyMap;
import static java.util.stream.Stream.empty;

public class IssueTrackingDelegator {
  private final PullRequestTrackerExecution pullRequestTracker;
  private final TrackerExecution tracker;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final ReferenceBranchTrackerExecution referenceBranchTracker;

  public IssueTrackingDelegator(PullRequestTrackerExecution pullRequestTracker, ReferenceBranchTrackerExecution referenceBranchTracker,
    TrackerExecution tracker, AnalysisMetadataHolder analysisMetadataHolder) {
    this.pullRequestTracker = pullRequestTracker;
    this.referenceBranchTracker = referenceBranchTracker;
    this.tracker = tracker;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public TrackingResult track(Component component, Input<DefaultIssue> rawInput, @Nullable Input<DefaultIssue> targetInput) {
    if (analysisMetadataHolder.isPullRequest()) {
      return standardResult(pullRequestTracker.track(component, rawInput, targetInput));
    }

    if (isFirstAnalysisSecondaryBranch()) {
      Tracking<DefaultIssue, DefaultIssue> tracking = referenceBranchTracker.track(component, rawInput);
      return new TrackingResult(tracking.getMatchedRaws(), emptyMap(), empty(), tracking.getUnmatchedRaws());
    }

    return standardResult(tracker.track(component, rawInput));
  }

  private static TrackingResult standardResult(Tracking<DefaultIssue, DefaultIssue> tracking) {
    return new TrackingResult(emptyMap(), tracking.getMatchedRaws(), tracking.getUnmatchedBases(), tracking.getUnmatchedRaws());
  }

  /**
   * Special case where we want to do the issue tracking with the reference branch, and copy matched issue to the current branch.
   */
  private boolean isFirstAnalysisSecondaryBranch() {
    if (analysisMetadataHolder.isFirstAnalysis()) {
      return !analysisMetadataHolder.getBranch().isMain();
    }
    return false;
  }
}
