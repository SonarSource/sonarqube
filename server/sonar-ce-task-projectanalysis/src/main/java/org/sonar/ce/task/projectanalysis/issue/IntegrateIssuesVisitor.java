/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.ReferenceBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache.CacheAppender;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.util.stream.MoreCollectors;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class IntegrateIssuesVisitor extends TypeAwareVisitorAdapter {

  private final ProtoIssueCache protoIssueCache;
  private final TrackerRawInputFactory rawInputFactory;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;
  private final IssueTrackingDelegator issueTracking;
  private final SiblingsIssueMerger issueStatusCopier;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids;
  private final PullRequestSourceBranchMerger pullRequestSourceBranchMerger;

  public IntegrateIssuesVisitor(
    ProtoIssueCache protoIssueCache,
    TrackerRawInputFactory rawInputFactory,
    IssueLifecycle issueLifecycle,
    IssueVisitors issueVisitors,
    IssueTrackingDelegator issueTracking,
    SiblingsIssueMerger issueStatusCopier,
    ReferenceBranchComponentUuids referenceBranchComponentUuids,
    PullRequestSourceBranchMerger pullRequestSourceBranchMerger) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.protoIssueCache = protoIssueCache;
    this.rawInputFactory = rawInputFactory;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
    this.issueTracking = issueTracking;
    this.issueStatusCopier = issueStatusCopier;
    this.referenceBranchComponentUuids = referenceBranchComponentUuids;
    this.pullRequestSourceBranchMerger = pullRequestSourceBranchMerger;
  }

  @Override
  public void visitAny(Component component) {
    try (CacheAppender<DefaultIssue> cacheAppender = protoIssueCache.newAppender()) {
      issueVisitors.beforeComponent(component);
      Input<DefaultIssue> rawInput = rawInputFactory.create(component);
      TrackingResult tracking = issueTracking.track(component, rawInput);
      fillNewOpenIssues(component, tracking.newIssues(), rawInput, cacheAppender);
      fillExistingOpenIssues(component, tracking.issuesToMerge(), cacheAppender);
      closeIssues(component, tracking.issuesToClose(), cacheAppender);
      copyIssues(component, tracking.issuesToCopy(), cacheAppender);
      issueVisitors.afterComponent(component);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to process issues of component '%s'", component.getDbKey()), e);
    }
  }

  private void fillNewOpenIssues(Component component, Stream<DefaultIssue> newIssues, Input<DefaultIssue> rawInput, CacheAppender<DefaultIssue> cacheAppender) {
    List<DefaultIssue> newIssuesList = newIssues
      .peek(issueLifecycle::initNewOpenIssue)
      .collect(MoreCollectors.toList());

    if (newIssuesList.isEmpty()) {
      return;
    }

    pullRequestSourceBranchMerger.tryMergeIssuesFromSourceBranchOfPullRequest(component, newIssuesList, rawInput);
    issueStatusCopier.tryMerge(component, newIssuesList);

    for (DefaultIssue issue : newIssuesList) {
      process(component, issue, cacheAppender);
    }
  }

  private void copyIssues(Component component, Map<DefaultIssue, DefaultIssue> matched, CacheAppender<DefaultIssue> cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.copyExistingOpenIssueFromBranch(raw, base, referenceBranchComponentUuids.getReferenceBranchName());
      process(component, raw, cacheAppender);
    }
  }

  private void fillExistingOpenIssues(Component component, Map<DefaultIssue, DefaultIssue> matched, CacheAppender<DefaultIssue> cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      process(component, raw, cacheAppender);
    }
  }

  private void closeIssues(Component component, Stream<DefaultIssue> issues, CacheAppender<DefaultIssue> cacheAppender) {
    issues.forEach(issue -> {
      // TODO should replace flag "beingClosed" by express call to transition "automaticClose"
      issue.setBeingClosed(true);
      // TODO manual issues -> was updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);. Is it a problem ?
      process(component, issue, cacheAppender);
    });
  }

  private void process(Component component, DefaultIssue issue, CacheAppender<DefaultIssue> cacheAppender) {
    issueLifecycle.doAutomaticTransition(issue);
    issueVisitors.onIssue(component, issue);
    if (issue.isNew() || issue.isChanged() || issue.isCopied() || issue.isNoLongerNewCodeReferenceIssue()) {
      cacheAppender.append(issue);
    }
  }

}
