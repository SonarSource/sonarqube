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

import java.util.Collection;
import java.util.Map;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.SimpleTracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class IssueStatusCopier {
  private final ResolvedShortBranchIssuesFactory shortBranchIssuesFactory;
  private final SimpleTracker<DefaultIssue, DefaultIssue> tracker;
  private final IssueLifecycle issueLifecycle;

  public IssueStatusCopier(ResolvedShortBranchIssuesFactory shortBranchIssuesFactory, IssueLifecycle issueLifecycle) {
    this(shortBranchIssuesFactory, new SimpleTracker<>(), issueLifecycle);
  }

  public IssueStatusCopier(ResolvedShortBranchIssuesFactory shortBranchIssuesFactory, SimpleTracker<DefaultIssue, DefaultIssue> tracker, IssueLifecycle issueLifecycle) {
    this.shortBranchIssuesFactory = shortBranchIssuesFactory;
    this.tracker = tracker;
    this.issueLifecycle = issueLifecycle;
  }

  public void updateStatus(Component component, Collection<DefaultIssue> newIssues) {
    Collection<DefaultIssue> shortBranchIssues = shortBranchIssuesFactory.create(component);
    Tracking<DefaultIssue, DefaultIssue> tracking = tracker.track(newIssues, shortBranchIssues);

    for (Map.Entry<DefaultIssue, DefaultIssue> e : tracking.getMatchedRaws().entrySet()) {
      issueLifecycle.copyResolution(e.getKey(), e.getValue());
    }
  }
}
