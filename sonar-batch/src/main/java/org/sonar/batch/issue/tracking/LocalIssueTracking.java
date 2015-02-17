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
package org.sonar.batch.issue.tracking;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchComponent;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.batch.index.BatchResource;
import org.sonar.batch.index.ResourceCache;
import org.sonar.batch.issue.IssueCache;
import org.sonar.batch.protocol.input.ProjectRepositories;
import org.sonar.batch.scan.LastLineHashes;
import org.sonar.batch.scan.filesystem.InputPathCache;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.workflow.IssueWorkflow;

import java.util.ArrayList;
import java.util.Collection;

public class LocalIssueTracking implements BatchComponent {

  private static final Logger LOG = LoggerFactory.getLogger(LocalIssueTracking.class);

  private final IssueCache issueCache;
  private final IssueTracking tracking;
  private final LastLineHashes lastLineHashes;
  private final IssueWorkflow workflow;
  private final IssueUpdater updater;
  private final IssueChangeContext changeContext;
  private final ActiveRules activeRules;
  private final InputPathCache inputPathCache;
  private final ResourceCache resourceCache;
  private final PreviousIssueRepository previousIssueCache;
  private final ProjectRepositories projectRepositories;

  public LocalIssueTracking(ResourceCache resourceCache, IssueCache issueCache, IssueTracking tracking,
    LastLineHashes lastLineHashes, IssueWorkflow workflow, IssueUpdater updater,
    ActiveRules activeRules, InputPathCache inputPathCache, PreviousIssueRepository previousIssueCache, ProjectRepositories projectRepositories) {
    this.resourceCache = resourceCache;
    this.issueCache = issueCache;
    this.tracking = tracking;
    this.lastLineHashes = lastLineHashes;
    this.workflow = workflow;
    this.updater = updater;
    this.inputPathCache = inputPathCache;
    this.previousIssueCache = previousIssueCache;
    this.projectRepositories = projectRepositories;
    this.changeContext = IssueChangeContext.createScan(((Project) resourceCache.getRoot().resource()).getAnalysisDate());
    this.activeRules = activeRules;
  }

  public void execute() {
    if (projectRepositories.lastAnalysisDate() == null) {
      LOG.debug("No previous analysis, skipping issue tracking");
      return;
    }

    previousIssueCache.load();

    for (BatchResource component : resourceCache.all()) {
      trackIssues(component);
    }
  }

  public void trackIssues(BatchResource component) {

    Collection<DefaultIssue> issues = Lists.newArrayList();
    for (Issue issue : issueCache.byComponent(component.resource().getEffectiveKey())) {
      issues.add((DefaultIssue) issue);
    }
    issueCache.clear(component.resource().getEffectiveKey());
    // issues = all the issues created by rule engines during this module scan and not excluded by filters

    // all the issues that are not closed in db before starting this module scan, including manual issues
    Collection<PreviousIssue> previousIssues = new ArrayList<>();
    for (org.sonar.batch.protocol.input.issues.PreviousIssue previousIssue : previousIssueCache.byComponent(component)) {
      previousIssues.add(new PreviousIssueFromWs(previousIssue));
    }

    SourceHashHolder sourceHashHolder = null;
    if (component.isFile()) {
      DefaultInputFile file = (DefaultInputFile) inputPathCache.getInputPath(component);
      if (file == null) {
        throw new IllegalStateException("Resource " + component.resource() + " was not found in InputPath cache");
      }
      sourceHashHolder = new SourceHashHolder((DefaultInputFile) file, lastLineHashes);
    }

    IssueTrackingResult trackingResult = tracking.track(sourceHashHolder, previousIssues, issues);

    // unmatched = issues that have been resolved + issues on disabled/removed rules + manual issues
    addUnmatched(trackingResult.unmatched(), sourceHashHolder, issues);

    mergeMatched(trackingResult);

    if (ResourceUtils.isRootProject(component.resource())) {
      // issues that relate to deleted components
      addIssuesOnDeletedComponents(issues);
    }

    for (DefaultIssue issue : issues) {
      workflow.doAutomaticTransition(issue, changeContext);
      issueCache.put(issue);
    }
  }

  @VisibleForTesting
  protected void mergeMatched(IssueTrackingResult result) {
    for (DefaultIssue issue : result.matched()) {
      org.sonar.batch.protocol.input.issues.PreviousIssue ref = ((PreviousIssueFromWs) result.matching(issue)).getDto();

      // invariant fields
      issue.setKey(ref.key());

      // non-persisted fields
      issue.setNew(false);
      issue.setEndOfLife(false);
      issue.setOnDisabledRule(false);

      // fields to update with old values
      issue.setResolution(ref.resolution());
      issue.setStatus(ref.status());
      issue.setAssignee(ref.assigneeLogin());
      issue.setCreationDate(ref.creationDate());

      if (ref.isManualSeverity()) {
        // Severity overriden by user
        issue.setSeverity(ref.severity());
      }
    }
  }

  private void addUnmatched(Collection<PreviousIssue> unmatchedIssues, SourceHashHolder sourceHashHolder, Collection<DefaultIssue> issues) {
    for (PreviousIssue unmatchedIssue : unmatchedIssues) {
      org.sonar.batch.protocol.input.issues.PreviousIssue unmatchedPreviousIssue = ((PreviousIssueFromWs) unmatchedIssue).getDto();
      DefaultIssue unmatched = toUnmatchedIssue(unmatchedPreviousIssue);
      if (unmatchedIssue.ruleKey().isManual() && !Issue.STATUS_CLOSED.equals(unmatchedPreviousIssue.status())) {
        relocateManualIssue(unmatched, unmatchedIssue, sourceHashHolder);
      }
      updateUnmatchedIssue(unmatched, false /* manual issues can be kept open */);
      issues.add(unmatched);
    }
  }

  private void addIssuesOnDeletedComponents(Collection<DefaultIssue> issues) {
    for (org.sonar.batch.protocol.input.issues.PreviousIssue previous : previousIssueCache.issuesOnMissingComponents()) {
      DefaultIssue dead = toUnmatchedIssue(previous);
      updateUnmatchedIssue(dead, true);
      issues.add(dead);
    }
  }

  private DefaultIssue toUnmatchedIssue(org.sonar.batch.protocol.input.issues.PreviousIssue previous) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(previous.key());
    issue.setStatus(previous.status());
    issue.setResolution(previous.resolution());
    issue.setMessage(previous.message());
    issue.setLine(previous.line());
    issue.setSeverity(previous.severity());
    issue.setAssignee(previous.assigneeLogin());
    issue.setComponentKey(previous.componentKey());
    issue.setManualSeverity(previous.isManualSeverity());
    issue.setCreationDate(previous.creationDate());
    issue.setRuleKey(RuleKey.of(previous.ruleRepo(), previous.ruleKey()));
    issue.setNew(false);
    return issue;
  }

  private void updateUnmatchedIssue(DefaultIssue issue, boolean forceEndOfLife) {
    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    issue.setNew(false);

    boolean manualIssue = issue.ruleKey().isManual();
    boolean isRemovedRule = activeRule == null;
    if (manualIssue) {
      issue.setEndOfLife(forceEndOfLife || isRemovedRule);
    } else {
      issue.setEndOfLife(true);
    }
    issue.setOnDisabledRule(isRemovedRule);
  }

  private void relocateManualIssue(DefaultIssue newIssue, PreviousIssue oldIssue, SourceHashHolder sourceHashHolder) {
    Integer previousLine = oldIssue.line();
    if (previousLine == null) {
      return;
    }

    Collection<Integer> newLinesWithSameHash = sourceHashHolder.getNewLinesMatching(previousLine);
    if (newLinesWithSameHash.isEmpty()) {
      if (previousLine > sourceHashHolder.getHashedSource().length()) {
        newIssue.setLine(null);
        updater.setStatus(newIssue, Issue.STATUS_CLOSED, changeContext);
        updater.setResolution(newIssue, Issue.RESOLUTION_REMOVED, changeContext);
        updater.setPastLine(newIssue, previousLine);
        updater.setPastMessage(newIssue, oldIssue.message(), changeContext);
      }
    } else if (newLinesWithSameHash.size() == 1) {
      Integer newLine = newLinesWithSameHash.iterator().next();
      newIssue.setLine(newLine);
      updater.setPastLine(newIssue, previousLine);
      updater.setPastMessage(newIssue, oldIssue.message(), changeContext);
    }
  }
}
