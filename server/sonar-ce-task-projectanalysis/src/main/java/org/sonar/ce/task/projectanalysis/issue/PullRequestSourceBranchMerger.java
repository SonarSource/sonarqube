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

import java.util.Collection;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;

public class PullRequestSourceBranchMerger {
  private final Tracker<DefaultIssue, DefaultIssue> tracker;
  private final IssueLifecycle issueLifecycle;
  private final TrackerSourceBranchInputFactory sourceBranchInputFactory;

  public PullRequestSourceBranchMerger(Tracker<DefaultIssue, DefaultIssue> tracker, IssueLifecycle issueLifecycle, TrackerSourceBranchInputFactory sourceBranchInputFactory) {
    this.tracker = tracker;
    this.issueLifecycle = issueLifecycle;
    this.sourceBranchInputFactory = sourceBranchInputFactory;
  }

  public void tryMergeIssuesFromSourceBranchOfPullRequest(Component component, Collection<DefaultIssue> newIssues, Input<DefaultIssue> rawInput) {
    if (sourceBranchInputFactory.hasSourceBranchAnalysis()) {
      Input<DefaultIssue> sourceBranchInput = sourceBranchInputFactory.createForSourceBranch(component);
      DefaultTrackingInput rawPrTrackingInput = new DefaultTrackingInput(newIssues, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
      Tracking<DefaultIssue, DefaultIssue> prTracking = tracker.trackNonClosed(rawPrTrackingInput, sourceBranchInput);
      for (Map.Entry<DefaultIssue, DefaultIssue> pair : prTracking.getMatchedRaws().entrySet()) {
        issueLifecycle.copyExistingIssueFromSourceBranchToPullRequest(pair.getKey(), pair.getValue());
      }
    }
  }
}
