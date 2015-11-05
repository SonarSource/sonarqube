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

import org.sonar.batch.issue.IssueTransformer;

import org.sonar.api.batch.fs.InputFile.Status;
import org.sonar.batch.analysis.DefaultAnalysisMode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.sonar.api.batch.BatchSide;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.issue.Issue;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.batch.index.BatchComponent;
import org.sonar.batch.protocol.output.BatchReport;
import org.sonar.batch.repository.ProjectRepositories;

@BatchSide
public class LocalIssueTracking {
  private final IssueTracking tracking;
  private final ServerLineHashesLoader lastLineHashes;
  private final ActiveRules activeRules;
  private final ServerIssueRepository serverIssueRepository;
  private final DefaultAnalysisMode mode;

  private boolean hasServerAnalysis;

  public LocalIssueTracking(IssueTracking tracking, ServerLineHashesLoader lastLineHashes,
    ActiveRules activeRules, ServerIssueRepository serverIssueRepository, ProjectRepositories projectRepositories, DefaultAnalysisMode mode) {
    this.tracking = tracking;
    this.lastLineHashes = lastLineHashes;
    this.serverIssueRepository = serverIssueRepository;
    this.mode = mode;
    this.activeRules = activeRules;
    this.hasServerAnalysis = projectRepositories.lastAnalysisDate() != null;
  }

  public void init() {
    if (hasServerAnalysis) {
      serverIssueRepository.load();
    }
  }

  public List<TrackedIssue> trackIssues(BatchComponent component, Set<BatchReport.Issue> rawIssues) {
    List<TrackedIssue> trackedIssues = Lists.newArrayList();
    if (hasServerAnalysis) {
      // all the issues that are not closed in db before starting this module scan, including manual issues
      Collection<ServerIssue> serverIssues = loadServerIssues(component);

      if (shouldCopyServerIssues(component)) {
        // raw issues should be empty, we just need to deal with server issues (SONAR-6931)
        copyServerIssues(serverIssues, trackedIssues);
      } else {

        SourceHashHolder sourceHashHolder = loadSourceHashes(component);

        IssueTrackingResult trackingResult = tracking.track(sourceHashHolder, serverIssues, rawIssues);

        // unmatched from server = issues that have been resolved + issues on disabled/removed rules + manual issues
        addUnmatchedFromServer(trackingResult.unmatched(), sourceHashHolder, trackedIssues);

        mergeMatched(component, trackingResult, trackedIssues, rawIssues);
      }
    }

    if (hasServerAnalysis && ResourceUtils.isRootProject(component.resource())) {
      // issues that relate to deleted components
      addIssuesOnDeletedComponents(trackedIssues);
    }

    return trackedIssues;
  }

  private boolean shouldCopyServerIssues(BatchComponent component) {
    if (!mode.scanAllFiles() && component.isFile()) {
      DefaultInputFile inputFile = (DefaultInputFile) component.inputComponent();
      if (inputFile.status() == Status.SAME) {
        return true;
      }
    }
    return false;
  }

  private void copyServerIssues(Collection<ServerIssue> serverIssues, List<TrackedIssue> trackedIssues) {
    for (ServerIssue serverIssue : serverIssues) {
      org.sonar.batch.protocol.input.BatchInput.ServerIssue unmatchedPreviousIssue = ((ServerIssueFromWs) serverIssue).getDto();
      TrackedIssue unmatched = IssueTransformer.toTrackedIssue(unmatchedPreviousIssue);

      ActiveRule activeRule = activeRules.find(unmatched.ruleKey());
      unmatched.setNew(false);

      if (activeRule == null) {
        // rule removed
        IssueTransformer.resolveRemove(unmatched);
      }

      trackedIssues.add(unmatched);
    }
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
  protected void mergeMatched(BatchComponent component, IssueTrackingResult result, List<TrackedIssue> trackedIssues, Collection<BatchReport.Issue> rawIssues) {
    for (BatchReport.Issue rawIssue : result.matched()) {
      rawIssues.remove(rawIssue);
      org.sonar.batch.protocol.input.BatchInput.ServerIssue ref = ((ServerIssueFromWs) result.matching(rawIssue)).getDto();

      TrackedIssue tracked = IssueTransformer.toTrackedIssue(component, rawIssue);

      // invariant fields
      tracked.setKey(ref.getKey());

      // non-persisted fields
      tracked.setNew(false);

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

  private void addUnmatchedFromServer(Collection<ServerIssue> unmatchedIssues, SourceHashHolder sourceHashHolder, Collection<TrackedIssue> issues) {
    for (ServerIssue unmatchedIssue : unmatchedIssues) {
      org.sonar.batch.protocol.input.BatchInput.ServerIssue unmatchedPreviousIssue = ((ServerIssueFromWs) unmatchedIssue).getDto();
      TrackedIssue unmatched = IssueTransformer.toTrackedIssue(unmatchedPreviousIssue);
      if (unmatchedIssue.ruleKey().isManual() && !Issue.STATUS_CLOSED.equals(unmatchedPreviousIssue.getStatus())) {
        relocateManualIssue(unmatched, unmatchedIssue, sourceHashHolder);
      }
      updateUnmatchedIssue(unmatched, false /* manual issues can be kept open */);
      issues.add(unmatched);
    }
  }

  private void addIssuesOnDeletedComponents(Collection<TrackedIssue> issues) {
    for (org.sonar.batch.protocol.input.BatchInput.ServerIssue previous : serverIssueRepository.issuesOnMissingComponents()) {
      TrackedIssue dead = IssueTransformer.toTrackedIssue(previous);
      updateUnmatchedIssue(dead, true);
      issues.add(dead);
    }
  }

  private void updateUnmatchedIssue(TrackedIssue issue, boolean forceEndOfLife) {
    ActiveRule activeRule = activeRules.find(issue.ruleKey());
    issue.setNew(false);

    boolean manualIssue = issue.ruleKey().isManual();
    boolean isRemovedRule = activeRule == null;

    if (isRemovedRule) {
      IssueTransformer.resolveRemove(issue);
    } else if (forceEndOfLife || !manualIssue) {
      IssueTransformer.close(issue);
    }
  }

  private static void relocateManualIssue(TrackedIssue newIssue, ServerIssue oldIssue, SourceHashHolder sourceHashHolder) {
    Integer previousLine = oldIssue.line();
    if (previousLine == null) {
      return;
    }

    Collection<Integer> newLinesWithSameHash = sourceHashHolder.getNewLinesMatching(previousLine);
    if (newLinesWithSameHash.isEmpty()) {
      if (previousLine > sourceHashHolder.getHashedSource().length()) {
        IssueTransformer.resolveRemove(newIssue);
      }
    } else if (newLinesWithSameHash.size() == 1) {
      Integer newLine = newLinesWithSameHash.iterator().next();
      newIssue.setStartLine(newLine);
      newIssue.setEndLine(newLine);
    }
  }
}
