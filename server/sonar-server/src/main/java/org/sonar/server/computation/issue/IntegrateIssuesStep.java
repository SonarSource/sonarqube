/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.computation.issue;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.step.ComputationStep;
import org.sonar.server.util.cache.DiskCache;

import static org.sonar.server.computation.component.DepthTraversalTypeAwareVisitor.Order.POST_ORDER;

public class IntegrateIssuesStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final TrackerExecution tracker;
  private final IssueCache issueCache;
  private final BaseIssuesLoader baseIssuesLoader;
  private final IssueLifecycle issueLifecycle;
  private final IssueListeners issueListeners;

  public IntegrateIssuesStep(TreeRootHolder treeRootHolder, TrackerExecution tracker, IssueCache issueCache,
    BaseIssuesLoader baseIssuesLoader, IssueLifecycle issueLifecycle,
    IssueListeners issueListeners) {
    this.treeRootHolder = treeRootHolder;
    this.tracker = tracker;
    this.issueCache = issueCache;
    this.baseIssuesLoader = baseIssuesLoader;
    this.issueLifecycle = issueLifecycle;
    this.issueListeners = issueListeners;
  }

  @Override
  public void execute() {
    // all the components that had issues before this analysis
    final Set<String> unprocessedComponentUuids = Sets.newHashSet(baseIssuesLoader.loadComponentUuids());

    new DepthTraversalTypeAwareVisitor(Component.Type.FILE, POST_ORDER) {
      @Override
      public void visitAny(Component component) {
        processIssues(component);
        unprocessedComponentUuids.remove(component.getUuid());
      }
    }.visit(treeRootHolder.getRoot());

    closeIssuesForDeletedComponentUuids(unprocessedComponentUuids);
  }

  private void processIssues(Component component) {
    Tracking<DefaultIssue, DefaultIssue> tracking = tracker.track(component);
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      issueListeners.beforeComponent(component, tracking);
      fillNewOpenIssues(component, tracking, cacheAppender);
      fillExistingOpenIssues(component, tracking, cacheAppender);
      closeUnmatchedBaseIssues(component, tracking, cacheAppender);
      issueListeners.afterComponent(component);
    } finally {
      cacheAppender.close();
    }
  }

  private void fillNewOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    Set<DefaultIssue> issues = tracking.getUnmatchedRaws();
    for (DefaultIssue issue : issues) {
      issueLifecycle.initNewOpenIssue(issue);
      issueListeners.beforeIssue(component, issue);
      process(component, issue, cacheAppender);
    }
  }

  private void fillExistingOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : tracking.getMatchedRaws().entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueListeners.beforeIssue(component, raw);
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      process(component, raw, cacheAppender);
    }
    for (Map.Entry<Integer, DefaultIssue> entry : tracking.getOpenManualIssuesByLine().entries()) {
      int line = entry.getKey();
      DefaultIssue manualIssue = entry.getValue();
      manualIssue.setLine(line == 0 ? null : line);
      issueListeners.beforeIssue(component, manualIssue);
      process(component, manualIssue, cacheAppender);
    }
  }

  private void closeUnmatchedBaseIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (DefaultIssue issue : tracking.getUnmatchedBases()) {
      // TODO should replace flag "beingClosed" by express call to transition "automaticClose"
      issue.setBeingClosed(true);
      // TODO manual issues -> was updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);. Is it a problem ?
      process(component, issue, cacheAppender);
    }
  }

  private void process(Component component, DefaultIssue issue, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    issueLifecycle.doAutomaticTransition(issue);
    issueListeners.onIssue(component, issue);
    cacheAppender.append(issue);
  }

  private void closeIssuesForDeletedComponentUuids(Set<String> deletedComponentUuids) {
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      for (String deletedComponentUuid : deletedComponentUuids) {
        List<DefaultIssue> issues = baseIssuesLoader.loadForComponentUuid(deletedComponentUuid);
        for (DefaultIssue issue : issues) {
          issue.setBeingClosed(true);
          issueLifecycle.doAutomaticTransition(issue);
          // TODO execute listeners ? Component is currently missing.
          cacheAppender.append(issue);
        }
      }
    } finally {
      cacheAppender.close();
    }
  }

  @Override
  public String getDescription() {
    return "Integrate issues";
  }

}
