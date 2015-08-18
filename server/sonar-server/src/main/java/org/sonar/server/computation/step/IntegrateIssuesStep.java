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
package org.sonar.server.computation.step;

import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sonar.api.utils.log.Loggers;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.DepthTraversalTypeAwareCrawler;
import org.sonar.server.computation.component.TreeRootHolder;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.computation.issue.BaseIssuesLoader;
import org.sonar.server.computation.issue.IssueCache;
import org.sonar.server.computation.issue.IssueLifecycle;
import org.sonar.server.computation.issue.IssueVisitors;
import org.sonar.server.computation.issue.TrackerExecution;
import org.sonar.server.util.cache.DiskCache;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class IntegrateIssuesStep implements ComputationStep {

  private final TreeRootHolder treeRootHolder;
  private final TrackerExecution tracker;
  private final IssueCache issueCache;
  private final BaseIssuesLoader baseIssuesLoader;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;

  public IntegrateIssuesStep(TreeRootHolder treeRootHolder, TrackerExecution tracker, IssueCache issueCache,
    BaseIssuesLoader baseIssuesLoader, IssueLifecycle issueLifecycle,
    IssueVisitors issueVisitors) {
    this.treeRootHolder = treeRootHolder;
    this.tracker = tracker;
    this.issueCache = issueCache;
    this.baseIssuesLoader = baseIssuesLoader;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
  }

  @Override
  public void execute() {
    // all the components that had issues before this analysis
    final Set<String> unprocessedComponentUuids = Sets.newHashSet(baseIssuesLoader.loadUuidsOfComponentsWithOpenIssues());

    new DepthTraversalTypeAwareCrawler(
      new TypeAwareVisitorAdapter(Component.Type.FILE, POST_ORDER) {
        @Override
        public void visitAny(Component component) {
          processIssues(component);
          unprocessedComponentUuids.remove(component.getUuid());
        }
      }).visit(treeRootHolder.getRoot());

    closeIssuesForDeletedComponentUuids(unprocessedComponentUuids);
  }

  private void processIssues(Component component) {
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      Tracking<DefaultIssue, DefaultIssue> tracking = tracker.track(component);
      issueVisitors.beforeComponent(component);
      fillNewOpenIssues(component, tracking, cacheAppender);
      fillExistingOpenIssues(component, tracking, cacheAppender);
      closeUnmatchedBaseIssues(component, tracking, cacheAppender);
      issueVisitors.afterComponent(component);
    } catch (Exception e) {
      Loggers.get(getClass()).error(String.format("Fail to process issues of %s", component.getKey()), e);
    } finally {
      cacheAppender.close();
    }
  }

  private void fillNewOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (DefaultIssue issue : tracking.getUnmatchedRaws()) {
      issueLifecycle.initNewOpenIssue(issue);
      process(component, issue, cacheAppender);
    }
  }

  private void fillExistingOpenIssues(Component component, Tracking<DefaultIssue, DefaultIssue> tracking, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : tracking.getMatchedRaws().entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      process(component, raw, cacheAppender);
    }
    for (Map.Entry<Integer, DefaultIssue> entry : tracking.getOpenManualIssuesByLine().entries()) {
      Integer line = entry.getKey();
      DefaultIssue manualIssue = entry.getValue();
      issueLifecycle.moveOpenManualIssue(manualIssue, line);
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
    issueVisitors.onIssue(component, issue);
    cacheAppender.append(issue);
  }

  private void closeIssuesForDeletedComponentUuids(Set<String> deletedComponentUuids) {
    DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender();
    try {
      for (String deletedComponentUuid : deletedComponentUuids) {
        List<DefaultIssue> issues = baseIssuesLoader.loadForComponentUuid(deletedComponentUuid);
        for (DefaultIssue issue : issues) {
          issue.setBeingClosed(true);
          // TODO should be renamed
          issue.setOnDisabledRule(false);
          issueLifecycle.doAutomaticTransition(issue);
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
