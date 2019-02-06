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

import java.util.stream.Stream;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.analysis.Branch;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.component.BranchType;

import static java.util.Collections.emptyMap;

public class IssueTrackingDelegator {
  private final ShortBranchOrPullRequestTrackerExecution shortBranchOrPullRequestTracker;
  private final TrackerExecution tracker;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MergeBranchTrackerExecution mergeBranchTracker;

  public IssueTrackingDelegator(ShortBranchOrPullRequestTrackerExecution shortBranchOrPullRequestTracker, MergeBranchTrackerExecution longBranchTracker,
                                TrackerExecution tracker, AnalysisMetadataHolder analysisMetadataHolder) {
    this.shortBranchOrPullRequestTracker = shortBranchOrPullRequestTracker;
    this.mergeBranchTracker = longBranchTracker;
    this.tracker = tracker;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public TrackingResult track(Component component) {
    if (analysisMetadataHolder.isSLBorPR()) {
      return standardResult(shortBranchOrPullRequestTracker.track(component));
    } else if (isFirstAnalysisSecondaryLongLivingBranch()) {
      Tracking<DefaultIssue, DefaultIssue> tracking = mergeBranchTracker.track(component);
      return new TrackingResult(tracking.getMatchedRaws(), emptyMap(), Stream.empty(), tracking.getUnmatchedRaws());
    } else {
      return standardResult(tracker.track(component));
    }
  }

  private static TrackingResult standardResult(Tracking<DefaultIssue, DefaultIssue> tracking) {
    return new TrackingResult(emptyMap(), tracking.getMatchedRaws(), tracking.getUnmatchedBases(), tracking.getUnmatchedRaws());
  }

  /**
   * Special case where we want to do the issue tracking with the merge branch, and copy matched issue to the current branch.
   */
  private boolean isFirstAnalysisSecondaryLongLivingBranch() {
    if (analysisMetadataHolder.isFirstAnalysis()) {
      Branch branch = analysisMetadataHolder.getBranch();
      return !branch.isMain() && branch.getType() == BranchType.LONG;
    }
    return false;
  }
}
