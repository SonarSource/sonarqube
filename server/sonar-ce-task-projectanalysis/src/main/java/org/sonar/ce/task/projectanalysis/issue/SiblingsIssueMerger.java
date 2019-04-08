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

import java.util.Collection;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.SimpleTracker;
import org.sonar.core.issue.tracking.Tracking;

public class SiblingsIssueMerger {
  private final SiblingsIssuesLoader siblingsIssuesLoader;
  private final SimpleTracker<DefaultIssue, SiblingIssue> tracker;
  private final IssueLifecycle issueLifecycle;

  public SiblingsIssueMerger(SiblingsIssuesLoader resolvedSiblingsIssuesLoader, IssueLifecycle issueLifecycle) {
    this(resolvedSiblingsIssuesLoader, new SimpleTracker<>(), issueLifecycle);
  }

  public SiblingsIssueMerger(SiblingsIssuesLoader siblingsIssuesLoader, SimpleTracker<DefaultIssue, SiblingIssue> tracker, IssueLifecycle issueLifecycle) {
    this.siblingsIssuesLoader = siblingsIssuesLoader;
    this.tracker = tracker;
    this.issueLifecycle = issueLifecycle;
  }

  /**
   * Look for all unclosed issues in branches/PR targeting the same long living branch, and run
   * a light issue tracking to find matches. Then merge issue attributes in the new issues. 
   */
  public void tryMerge(Component component, Collection<DefaultIssue> newIssues) {
    Collection<SiblingIssue> siblingIssues = siblingsIssuesLoader.loadCandidateSiblingIssuesForMerging(component);
    Tracking<DefaultIssue, SiblingIssue> tracking = tracker.track(newIssues, siblingIssues);

    Map<DefaultIssue, SiblingIssue> matchedRaws = tracking.getMatchedRaws();

    Map<SiblingIssue, DefaultIssue> defaultIssues = siblingsIssuesLoader.loadDefaultIssuesWithChanges(matchedRaws.values());

    for (Map.Entry<DefaultIssue, SiblingIssue> e : matchedRaws.entrySet()) {
      SiblingIssue issue = e.getValue();
      issueLifecycle.mergeConfirmedOrResolvedFromShortLivingBranchOrPr(e.getKey(), defaultIssues.get(issue), issue.getBranchType(), issue.getBranchKey());
    }
  }
}
