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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.component.Component;
import org.sonar.server.computation.component.CrawlerDepthLimit;
import org.sonar.server.computation.component.TypeAwareVisitorAdapter;
import org.sonar.server.util.cache.DiskCache;

import static org.sonar.server.computation.component.ComponentVisitor.Order.POST_ORDER;

public class IntegrateIssuesVisitor extends TypeAwareVisitorAdapter {

  private final TrackerExecution tracker;
  private final IssueCache issueCache;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;
  private final MutableComponentIssuesRepository componentIssuesRepository;
  private final ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues;

  private final List<DefaultIssue> componentIssues = new ArrayList<>();

  public IntegrateIssuesVisitor(TrackerExecution tracker, IssueCache issueCache, IssueLifecycle issueLifecycle, IssueVisitors issueVisitors,
                                ComponentsWithUnprocessedIssues componentsWithUnprocessedIssues, MutableComponentIssuesRepository componentIssuesRepository) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.tracker = tracker;
    this.issueCache = issueCache;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
    this.componentsWithUnprocessedIssues = componentsWithUnprocessedIssues;
    this.componentIssuesRepository = componentIssuesRepository;
  }

  @Override
  public void visitAny(Component component) {
    componentIssues.clear();
    processIssues(component);
    componentsWithUnprocessedIssues.remove(component.getUuid());
    componentIssuesRepository.setIssues(component, componentIssues);
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
      throw new IllegalStateException(String.format("Fail to process issues of component '%s'", component.getKey()), e);
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
    componentIssues.add(issue);
  }

}
