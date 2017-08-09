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

import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.component.BranchType;
import org.sonar.server.computation.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.server.computation.task.projectanalysis.analysis.Branch;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class IssueTrackingDelegator {
  private final ShortBranchTrackerExecution shortBranchTracker;
  private final TrackerExecution tracker;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public IssueTrackingDelegator(ShortBranchTrackerExecution shortBranchTracker, TrackerExecution tracker, AnalysisMetadataHolder analysisMetadataHolder) {
    this.shortBranchTracker = shortBranchTracker;
    this.tracker = tracker;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  private boolean isShortLivingBranch() {
    java.util.Optional<Branch> branch = analysisMetadataHolder.getBranch();
    return branch.isPresent() && branch.get().getType() == BranchType.SHORT;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component) {
    if (isShortLivingBranch()) {
      return shortBranchTracker.track(component);
    } else {
      return tracker.track(component);
    }
  }
}
