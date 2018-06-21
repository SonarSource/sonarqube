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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.Collection;
import java.util.Map;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.SimpleTracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class ShortBranchIssueMerger {
  private final ShortBranchIssuesLoader shortBranchIssuesLoader;
  private final SimpleTracker<DefaultIssue, ShortBranchIssue> tracker;
  private final IssueLifecycle issueLifecycle;

  public ShortBranchIssueMerger(ShortBranchIssuesLoader resolvedShortBranchIssuesLoader, IssueLifecycle issueLifecycle) {
    this(resolvedShortBranchIssuesLoader, new SimpleTracker<>(), issueLifecycle);
  }

  public ShortBranchIssueMerger(ShortBranchIssuesLoader shortBranchIssuesLoader, SimpleTracker<DefaultIssue, ShortBranchIssue> tracker, IssueLifecycle issueLifecycle) {
    this.shortBranchIssuesLoader = shortBranchIssuesLoader;
    this.tracker = tracker;
    this.issueLifecycle = issueLifecycle;
  }

  /**
   * Look for all resolved/confirmed issues in short living branches targeting the current long living branch, and run
   * a light issue tracking to find matches. Then merge issue attributes in the new issues. 
   */
  public void tryMerge(Component component, Collection<DefaultIssue> newIssues) {
    Collection<ShortBranchIssue> shortBranchIssues = shortBranchIssuesLoader.loadCandidateIssuesForMergingInTargetBranch(component);
    Tracking<DefaultIssue, ShortBranchIssue> tracking = tracker.track(newIssues, shortBranchIssues);

    Map<DefaultIssue, ShortBranchIssue> matchedRaws = tracking.getMatchedRaws();

    Map<ShortBranchIssue, DefaultIssue> defaultIssues = shortBranchIssuesLoader.loadDefaultIssuesWithChanges(matchedRaws.values());

    for (Map.Entry<DefaultIssue, ShortBranchIssue> e : matchedRaws.entrySet()) {
      ShortBranchIssue issue = e.getValue();
      issueLifecycle.mergeConfirmedOrResolvedFromShortLivingBranch(e.getKey(), defaultIssues.get(issue), issue.getBranchName());
    }
  }
}
