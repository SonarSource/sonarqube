/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;

public class PullRequestTrackerExecution {
  private final TrackerBaseInputFactory baseInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;
  private final NewLinesRepository newLinesRepository;
  private final TrackerTargetBranchInputFactory targetInputFactory;

  public PullRequestTrackerExecution(TrackerBaseInputFactory baseInputFactory, TrackerTargetBranchInputFactory targetInputFactory,
    Tracker<DefaultIssue, DefaultIssue> tracker, NewLinesRepository newLinesRepository) {
    this.baseInputFactory = baseInputFactory;
    this.targetInputFactory = targetInputFactory;
    this.tracker = tracker;
    this.newLinesRepository = newLinesRepository;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component, Input<DefaultIssue> rawInput) {
    // Step 1: only keep issues on changed lines
    List<DefaultIssue> filteredRaws = keepIssuesHavingAtLeastOneLocationOnChangedLines(component, rawInput.getIssues());
    Input<DefaultIssue> unmatchedRawsAfterChangedLineFiltering = createInput(rawInput, filteredRaws);

    // Step 2: remove issues that are resolved in the target branch
    Input<DefaultIssue> unmatchedRawsAfterTargetResolvedTracking;
    if (targetInputFactory.hasTargetBranchAnalysis()) {
      Input<DefaultIssue> targetInput = targetInputFactory.createForTargetBranch(component);
      List<DefaultIssue> resolvedTargetIssues = targetInput.getIssues().stream().filter(i -> Issue.STATUS_RESOLVED.equals(i.status())).toList();
      Input<DefaultIssue> resolvedTargetInput = createInput(targetInput, resolvedTargetIssues);
      Tracking<DefaultIssue, DefaultIssue> prResolvedTracking = tracker.trackNonClosed(unmatchedRawsAfterChangedLineFiltering, resolvedTargetInput);
      unmatchedRawsAfterTargetResolvedTracking = createInput(rawInput, prResolvedTracking.getUnmatchedRaws().toList());
    } else {
      unmatchedRawsAfterTargetResolvedTracking = unmatchedRawsAfterChangedLineFiltering;
    }

    // Step 3: track issues with previous analysis of the current PR
    Input<DefaultIssue> previousAnalysisInput = baseInputFactory.create(component);
    return tracker.trackNonClosed(unmatchedRawsAfterTargetResolvedTracking, previousAnalysisInput);
  }

  private static Input<DefaultIssue> createInput(Input<DefaultIssue> input, Collection<DefaultIssue> issues) {
    return new DefaultTrackingInput(issues, input.getLineHashSequence(), input.getBlockHashSequence());
  }

  private List<DefaultIssue> keepIssuesHavingAtLeastOneLocationOnChangedLines(Component component, Collection<DefaultIssue> issues) {
    if (component.getType() != Component.Type.FILE) {
      return Collections.emptyList();
    }
    final Optional<Set<Integer>> newLinesOpt = newLinesRepository.getNewLines(component);
    if (!newLinesOpt.isPresent()) {
      return Collections.emptyList();
    }
    final Set<Integer> newLines = newLinesOpt.get();
    return issues.stream()
      .filter(i -> IssueLocations.allLinesFor(i, component.getUuid()).anyMatch(newLines::contains))
      .toList();
  }

}
