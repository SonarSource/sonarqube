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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.sonar.api.rules.RuleType;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.CrawlerDepthLimit;
import org.sonar.ce.task.projectanalysis.component.MergeBranchComponentUuids;
import org.sonar.ce.task.projectanalysis.component.TypeAwareVisitorAdapter;
import org.sonar.ce.task.projectanalysis.util.cache.DiskCache;
import org.sonar.core.issue.DefaultIssue;

import static org.sonar.ce.task.projectanalysis.component.ComponentVisitor.Order.POST_ORDER;

public class IntegrateIssuesVisitor extends TypeAwareVisitorAdapter {

  private final IssueCache issueCache;
  private final IssueLifecycle issueLifecycle;
  private final IssueVisitors issueVisitors;
  private final IssueTrackingDelegator issueTracking;
  private final ShortBranchIssueMerger issueStatusCopier;
  private final AnalysisMetadataHolder analysisMetadataHolder;
  private final MergeBranchComponentUuids mergeBranchComponentUuids;

  public IntegrateIssuesVisitor(IssueCache issueCache, IssueLifecycle issueLifecycle, IssueVisitors issueVisitors,
    AnalysisMetadataHolder analysisMetadataHolder, IssueTrackingDelegator issueTracking, ShortBranchIssueMerger issueStatusCopier,
    MergeBranchComponentUuids mergeBranchComponentUuids) {
    super(CrawlerDepthLimit.FILE, POST_ORDER);
    this.issueCache = issueCache;
    this.issueLifecycle = issueLifecycle;
    this.issueVisitors = issueVisitors;
    this.analysisMetadataHolder = analysisMetadataHolder;
    this.issueTracking = issueTracking;
    this.issueStatusCopier = issueStatusCopier;
    this.mergeBranchComponentUuids = mergeBranchComponentUuids;
  }

  @Override
  public void visitAny(Component component) {
    try (DiskCache<DefaultIssue>.DiskAppender cacheAppender = issueCache.newAppender()) {
      issueVisitors.beforeComponent(component);
      TrackingResult tracking = issueTracking.track(component);
      fillNewOpenIssues(component, tracking.newIssues(), cacheAppender);
      fillExistingOpenIssues(component, tracking.issuesToMerge(), cacheAppender);
      closeIssues(component, tracking.issuesToClose(), cacheAppender);
      copyIssues(component, tracking.issuesToCopy(), cacheAppender);
      issueVisitors.afterComponent(component);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Fail to process issues of component '%s'", component.getDbKey()), e);
    }
  }

  private void fillNewOpenIssues(Component component, Stream<DefaultIssue> newIssues, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    List<DefaultIssue> list = new ArrayList<>();

    newIssues.forEach(issue -> {
      issueLifecycle.initNewOpenIssue(issue);
      if (analysisMetadataHolder.isLongLivingBranch() || issue.type() != RuleType.SECURITY_HOTSPOT) {
        list.add(issue);
      }
    });

    if (list.isEmpty()) {
      return;
    }

    if (analysisMetadataHolder.isLongLivingBranch()) {
      issueStatusCopier.tryMerge(component, list);
    }

    for (DefaultIssue issue : list) {
      process(component, issue, cacheAppender);
    }
  }

  private void copyIssues(Component component, Map<DefaultIssue, DefaultIssue> matched, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.copyExistingOpenIssueFromLongLivingBranch(raw, base, mergeBranchComponentUuids.getMergeBranchName());
      process(component, raw, cacheAppender);
    }
  }

  private void fillExistingOpenIssues(Component component, Map<DefaultIssue, DefaultIssue> matched, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    for (Map.Entry<DefaultIssue, DefaultIssue> entry : matched.entrySet()) {
      DefaultIssue raw = entry.getKey();
      DefaultIssue base = entry.getValue();
      issueLifecycle.mergeExistingOpenIssue(raw, base);
      if (analysisMetadataHolder.isLongLivingBranch() || raw.type() != RuleType.SECURITY_HOTSPOT) {
        process(component, raw, cacheAppender);
      }
    }
  }

  private void closeIssues(Component component, Stream<DefaultIssue> issues, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    issues.forEach(issue -> {
      // TODO should replace flag "beingClosed" by express call to transition "automaticClose"
      issue.setBeingClosed(true);
      // TODO manual issues -> was updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);. Is it a problem ?
      process(component, issue, cacheAppender);
    });
  }

  private void process(Component component, DefaultIssue issue, DiskCache<DefaultIssue>.DiskAppender cacheAppender) {
    issueLifecycle.doAutomaticTransition(issue);
    issueVisitors.onIssue(component, issue);
    cacheAppender.append(issue);
  }

}
