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

import org.sonar.batch.repository.ProjectRepositories;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.KeyValueFormat;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.index.BatchComponentCache;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.protocol.output.BatchReportReader;
import org.sonar.batch.report.ReportPublisher;
import org.sonar.core.component.ComponentKeys;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;

@BatchSide
public class LocalIssueTracking {
  private final IssueTracking tracking;
  private final ServerLineHashesLoader lastLineHashes;
  private final IssueUpdater updater;
  private final IssueChangeContext changeContext;
  private final ActiveRules activeRules;
  private final ServerIssueRepository serverIssueRepository;
  private final Date analysisDate;

  private boolean hasServerAnalysis;

  public LocalIssueTracking(BatchComponentCache resourceCache, IssueTracking tracking, ServerLineHashesLoader lastLineHashes, IssueUpdater updater,
    ActiveRules activeRules, ServerIssueRepository serverIssueRepository, ProjectRepositories projectRepositories, ReportPublisher reportPublisher) {
    this.tracking = tracking;
    this.lastLineHashes = lastLineHashes;
    this.updater = updater;
    this.serverIssueRepository = serverIssueRepository;
    this.analysisDate = ((Project) resourceCache.getRoot().resource()).getAnalysisDate();
    this.changeContext = IssueChangeContext.createScan(analysisDate);
    this.activeRules = activeRules;
    this.hasServerAnalysis = projectRepositories.lastAnalysisDate() != null;
  }

  public void init() {
    if (hasServerAnalysis) {
      serverIssueRepository.load();
    }
  }

  public List<DefaultIssue> trackIssues(BatchReportReader reader, BatchComponent component, Set<BatchReport.Issue> rawIssues) {
    List<DefaultIssue> trackedIssues = Lists.newArrayList();
    if (hasServerAnalysis) {

      // all the issues that are not closed in db before starting this module scan, including manual issues
      Collection<ServerIssue> serverIssues = loadServerIssues(component);

      SourceHashHolder sourceHashHolder = loadSourceHashes(component);

      IssueTrackingResult trackingResult = tracking.track(sourceHashHolder, serverIssues, rawIssues);

      // unmatched from server = issues that have been resolved + issues on disabled/removed rules + manual issues
      addUnmatchedFromServer(trackingResult.unmatched(), sourceHashHolder, trackedIssues);

      mergeMatched(component, trackingResult, trackedIssues, rawIssues);
    }

    if (hasServerAnalysis && ResourceUtils.isRootProject(component.resource())) {
      // issues that relate to deleted components
      addIssuesOnDeletedComponents(trackedIssues);
    }

    return trackedIssues;
  }

  private DefaultIssue toTracked(BatchComponent component, BatchReport.Issue rawIssue) {
    DefaultIssue trackedIssue = new org.sonar.core.issue.DefaultIssueBuilder()
      .componentKey(component.key())
      .projectKey("unused")
      .ruleKey(RuleKey.of(rawIssue.getRuleRepository(), rawIssue.getRuleKey()))
      .effortToFix(rawIssue.hasEffortToFix() ? rawIssue.getEffortToFix() : null)
      .line(rawIssue.hasLine() ? rawIssue.getLine() : null)
      .message(rawIssue.hasMsg() ? rawIssue.getMsg() : null)
      .severity(rawIssue.getSeverity().name())
      .build();
    trackedIssue.setAttributes(rawIssue.hasAttributes() ? KeyValueFormat.parse(rawIssue.getAttributes()) : Collections.<String, String>emptyMap());
    return trackedIssue;
  }

  @CheckForNull
  private SourceHashHolder loadSourceHashes(BatchComponent component) {
    SourceHashHolder sourceHashHolder = null;
    if (component.isFile()) {
      DefaultInputFile file = (DefaultInputFile) component.inputComponent();
      if (file == null) {
        throw new IllegalStateException("Resource " + component.resource() + " was not found in InputPath cache");
      }
      sourceHashHolder = new SourceHashHolder(file, lastLineHashes);
    }
    return sourceHashHolder;
  }

  private Collection<ServerIssue> loadServerIssues(BatchComponent component) {
    Collection<ServerIssue> serverIssues = new ArrayList<>();
    for (org.sonar.batch.protocol.input.BatchInput.ServerIssue previousIssue : serverIssueRepository.byComponent(component)) {
      serverIssues.add(new ServerIssueFromWs(previousIssue));
    }
    return serverIssues;
  }

  @VisibleForTesting
  protected void mergeMatched(BatchComponent component, IssueTrackingResult result, List<DefaultIssue> trackedIssues, Collection<BatchReport.Issue> rawIssues) {
    for (BatchReport.Issue rawIssue : result.matched()) {
      rawIssues.remove(rawIssue);
      org.sonar.batch.protocol.input.BatchInput.ServerIssue ref = ((ServerIssueFromWs) result.matching(rawIssue)).getDto();

      DefaultIssue tracked = toTracked(component, rawIssue);

      // invariant fields
      tracked.setKey(ref.getKey());

      // non-persisted fields
      tracked.setNew(false);
      tracked.setBeingClosed(false);
      tracked.setOnDisabledRule(false);

      // fields to update with old values
      tracked.setResolution(ref.hasResolution() ? ref.getResolution() : null);
      tracked.setStatus(ref.getStatus());
      tracked.setAssignee(ref.hasAssigneeLogin() ? ref.getAssigneeLogin() : null);
      tracked.setCreationDate(new Date(ref.getCreationDate()));

      if (ref.getManualSeverity()) {
        // Severity overriden by user
        tracked.setSeverity(ref.getSeverity().name());
      }
      trackedIssues.add(tracked);
    }
  }

  private void addUnmatchedFromServer(Collection<ServerIssue> unmatchedIssues, SourceHashHolder sourceHashHolder, Collection<DefaultIssue> issues) {
    for (ServerIssue unmatchedIssue : unmatchedIssues) {
      org.sonar.batch.protocol.input.BatchInput.ServerIssue unmatchedPreviousIssue = ((ServerIssueFromWs) unmatchedIssue).getDto();
      DefaultIssue unmatched = toUnmatchedIssue(unmatchedPreviousIssue);
      if (unmatchedIssue.ruleKey().isManual() && !Issue.STATUS_CLOSED.equals(unmatchedPreviousIssue.getStatus())) {
        relocateManualIssue(unmatched, unmatchedIssue, sourceHashHolder);
      }
      updateUnmatchedIssue(unmatched, false /* manual issues can be kept open */);
      issues.add(unmatched);
    }
  }

  private void addIssuesOnDeletedComponents(Collection<DefaultIssue> issues) {
    for (org.sonar.batch.protocol.input.BatchInput.ServerIssue previous : serverIssueRepository.issuesOnMissingComponents()) {
      DefaultIssue dead = toUnmatchedIssue(previous);
      updateUnmatchedIssue(dead, true);
      issues.add(dead);
    }
  }

  private DefaultIssue toUnmatchedIssue(org.sonar.batch.protocol.input.BatchInput.ServerIssue serverIssue) {
    DefaultIssue issue = new DefaultIssue();
    issue.setKey(serverIssue.getKey());
    issue.setStatus(serverIssue.getStatus());
    issue.setResolution(serverIssue.hasResolution() ? serverIssue.getResolution() : null);
    issue.setMessage(serverIssue.hasMsg() ? serverIssue.getMsg() : null);
    issue.setLine(serverIssue.hasLine() ? serverIssue.getLine() : null);
    issue.setSeverity(serverIssue.getSeverity().name());
    issue.setAssignee(serverIssue.hasAssigneeLogin() ? serverIssue.getAssigneeLogin() : null);
    issue.setComponentKey(ComponentKeys.createEffectiveKey(serverIssue.getModuleKey(), serverIssue.hasPath() ? serverIssue.getPath() : null));
    issue.setManualSeverity(serverIssue.getManualSeverity());
    issue.setCreationDate(new Date(serverIssue.getCreationDate()));
    issue.setRuleKey(RuleKey.of(serverIssue.getRuleRepository(), serverIssue.getRuleKey()));
    issue.setNew(false);
    return issue;
  }

  private void updateUnmatchedIssue(DefaultIssue issue, boolean forceEndOfLife) {
    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    issue.setNew(false);

    boolean manualIssue = issue.ruleKey().isManual();
    boolean isRemovedRule = activeRule == null;
    if (manualIssue) {
      issue.setBeingClosed(forceEndOfLife || isRemovedRule);
    } else {
      issue.setBeingClosed(true);
    }
    issue.setOnDisabledRule(isRemovedRule);
  }

  private void relocateManualIssue(DefaultIssue newIssue, ServerIssue oldIssue, SourceHashHolder sourceHashHolder) {
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
