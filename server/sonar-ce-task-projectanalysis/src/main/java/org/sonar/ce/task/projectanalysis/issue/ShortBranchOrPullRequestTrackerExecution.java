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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.source.NewLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.core.util.stream.MoreCollectors;

public class ShortBranchOrPullRequestTrackerExecution {
  private final TrackerBaseInputFactory baseInputFactory;
  private final TrackerRawInputFactory rawInputFactory;
  private final TrackerMergeOrTargetBranchInputFactory mergeInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;
  private final NewLinesRepository newLinesRepository;

  public ShortBranchOrPullRequestTrackerExecution(TrackerBaseInputFactory baseInputFactory, TrackerRawInputFactory rawInputFactory,
    TrackerMergeOrTargetBranchInputFactory mergeInputFactory, Tracker<DefaultIssue, DefaultIssue> tracker, NewLinesRepository newLinesRepository) {
    this.baseInputFactory = baseInputFactory;
    this.rawInputFactory = rawInputFactory;
    this.mergeInputFactory = mergeInputFactory;
    this.tracker = tracker;
    this.newLinesRepository = newLinesRepository;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component) {
    Input<DefaultIssue> rawInput = rawInputFactory.create(component);
    Input<DefaultIssue> previousAnalysisInput = baseInputFactory.create(component);

    // Step 1: track issues with merge branch (= long living)
    Input<DefaultIssue> unmatchedRawsAfterMergeBranchTracking;
    if (mergeInputFactory.hasMergeBranchAnalysis()) {
      Input<DefaultIssue> mergeInput = mergeInputFactory.createForMergeBranch(component);
      Tracking<DefaultIssue, DefaultIssue> mergeTracking = tracker.trackNonClosed(rawInput, mergeInput);
      List<DefaultIssue> unmatchedRaws = mergeTracking.getUnmatchedRaws().collect(MoreCollectors.toList());
      unmatchedRawsAfterMergeBranchTracking = new DefaultTrackingInput(unmatchedRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
    } else {
      unmatchedRawsAfterMergeBranchTracking = rawInput;
    }

    // Step 2: track remaining unmatched issues with target branch
    Input<DefaultIssue> unmatchedRawsAfterTargetBranchTracking;
    if (mergeInputFactory.hasTargetBranchAnalysis() && mergeInputFactory.areTargetAndMergeBranchesDifferent()) {
      Input<DefaultIssue> targetInput = mergeInputFactory.createForTargetBranch(component);
      Tracking<DefaultIssue, DefaultIssue> mergeTracking = tracker.trackNonClosed(unmatchedRawsAfterMergeBranchTracking, targetInput);
      List<DefaultIssue> unmatchedRaws = mergeTracking.getUnmatchedRaws().collect(MoreCollectors.toList());
      unmatchedRawsAfterTargetBranchTracking = new DefaultTrackingInput(unmatchedRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
    } else {
      unmatchedRawsAfterTargetBranchTracking = unmatchedRawsAfterMergeBranchTracking;
    }

    // Step 3: if there is no analysis or merge or target branch, keep only issues on changed lines
    Input<DefaultIssue> unmatchedRawsAfterChangedLineFiltering;
    if (!mergeInputFactory.hasTargetBranchAnalysis() || !mergeInputFactory.hasMergeBranchAnalysis()) {
      List<DefaultIssue> filteredRaws = keepIssuesHavingAtLeastOneLocationOnChangedLines(component, unmatchedRawsAfterTargetBranchTracking.getIssues());
      unmatchedRawsAfterChangedLineFiltering = new DefaultTrackingInput(filteredRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
    } else {
      unmatchedRawsAfterChangedLineFiltering = unmatchedRawsAfterTargetBranchTracking;
    }

    // Step 4: track issues of previous analysis of the current branch/PR
    return tracker.trackNonClosed(unmatchedRawsAfterChangedLineFiltering, previousAnalysisInput);
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
    return issues
      .stream()
      .filter(i -> IssueLocations.allLinesFor(i, component.getUuid()).anyMatch(newLines::contains))
      .collect(Collectors.toList());
  }

}
