/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.FileStatuses;
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
  private final TrackerBaseInputFactory baseInputFactory;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;
  private final IssueTrackingDelegator issueTracking;
  private final SiblingsIssueMerger issueStatusCopier;
  private final ReferenceBranchComponentUuids referenceBranchComponentUuids;
  private final PullRequestSourceBranchMerger pullRequestSourceBranchMerger;
  private final FileStatuses fileStatuses;

  public IntegrateIssuesVisitor(
    ProtoIssueCache protoIssueCache,
    TrackerRawInputFactory rawInputFactory,
    TrackerBaseInputFactory baseInputFactory,
    IssueLifecycle issueLifecycle,
    IssueVisitors issueVisitors,
    IssueTrackingDelegator issueTracking,
    SiblingsIssueMerger issueStatusCopier,
    ReferenceBranchComponentUuids referenceBranchComponentUuids,
    PullRequestSourceBranchMerger pullRequestSourceBranchMerger,
    FileStatuses fileStatuses) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.protoIssueCache = protoIssueCache;
    this.rawInputFactory = rawInputFactory;
    this.baseInputFactory = baseInputFactory;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
    this.issueTracking = issueTracking;
    this.issueStatusCopier = issueStatusCopier;
    this.referenceBranchComponentUuids = referenceBranchComponentUuids;
    this.pullRequestSourceBranchMerger = pullRequestSourceBranchMerger;
    this.fileStatuses = fileStatuses;
  }

  @Override
  public void visitAny(Component component) {
    try (CacheAppender<DefaultIssue> cacheAppender = protoIssueCache.newAppender()) {
      issueVisitors.beforeComponent(component);

      if (fileStatuses.isDataUnchanged(component)) {
        // we assume there's a previous analysis of the same branch
        Input<DefaultIssue> baseIssues = baseInputFactory.create(component);
        var issues = new LinkedList<>(baseIssues.getIssues());
        processIssues(component, issues);
        issueVisitors.beforeCaching(component);
        appendIssuesToCache(cacheAppender, issues);
      } else {
        Input<DefaultIssue> rawInput = rawInputFactory.create(component);
        TrackingResult tracking = issueTracking.track(component, rawInput);
        var newOpenIssues = fillNewOpenIssues(component, tracking.newIssues(), rawInput);
        var existingOpenIssues = fillExistingOpenIssues(tracking.issuesToMerge());
        var closedIssues = closeIssues(tracking.issuesToClose());
        var copiedIssues = copyIssues(tracking.issuesToCopy());

        var issues = Stream.of(newOpenIssues, existingOpenIssues, closedIssues, copiedIssues)
          .flatMap(Collection::stream)
          .toList();
        processIssues(component, issues);
        issueVisitors.beforeCaching(component);
        appendIssuesToCache(cacheAppender, issues);
      }
      issueVisitors.afterComponent(component);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to process issues of component '%s'", component.getKey()), e);
    }
  }

  private void processIssues(Component component, Collection<DefaultIssue> issues) {
    issues.forEach(issue -> processIssue(component, issue));
  }

  private List<DefaultIssue> fillNewOpenIssues(Component component, Stream<DefaultIssue> newIssues, Input<DefaultIssue> rawInput) {
    List<DefaultIssue> newIssuesList = newIssues
      .peek(issueLifecycle::initNewOpenIssue)
      .collect(MoreCollectors.toList());

    if (newIssuesList.isEmpty()) {
      return newIssuesList;
    }

    pullRequestSourceBranchMerger.tryMergeIssuesFromSourceBranchOfPullRequest(component, newIssuesList, rawInput);
    issueStatusCopier.tryMerge(component, newIssuesList);
    return newIssuesList;
  }

  private List<DefaultIssue> fillExistingOpenIssues(Map<DefaultIssue, DefaultIssue> matched) {
    List<DefaultIssue> newIssuesList = new LinkedList<>();
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      newIssuesList.add(raw);
    }
    return newIssuesList;
  }

  private static List<DefaultIssue> closeIssues(Stream<DefaultIssue> issues) {
    return issues.map(issue ->
    // TODO should replace flag "beingClosed" by express call to transition "automaticClose"
    issue.setBeingClosed(true)
    // TODO manual issues -> was updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);. Is it a problem ?
    ).toList();
  }

  private List<DefaultIssue> copyIssues(Map<DefaultIssue, DefaultIssue> matched) {
    List<DefaultIssue> newIssuesList = new LinkedList<>();
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.copyExistingOpenIssueFromBranch(raw, base, referenceBranchComponentUuids.getReferenceBranchName());
      newIssuesList.add(raw);
    }
    return newIssuesList;
  }

  private void processIssue(Component component, DefaultIssue issue) {
    issueLifecycle.doAutomaticTransition(issue);
    issueVisitors.onIssue(component, issue);
  }

  private static void appendIssuesToCache(CacheAppender<DefaultIssue> cacheAppender, Collection<DefaultIssue> issues) {
    issues.forEach(issue -> appendIssue(issue, cacheAppender));
  }

  private static void appendIssue(DefaultIssue issue, CacheAppender<DefaultIssue> cacheAppender) {
    if (issue.isNew() || issue.isChanged() || issue.isCopied() || issue.isNoLongerNewCodeReferenceIssue() || issue.isToBeMigratedAsNewCodeReferenceIssue()) {
      cacheAppender.append(issue);
    }
  }

}
