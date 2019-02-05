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
  private final TrackerMergeBranchInputFactory mergeInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;
  private final NewLinesRepository newLinesRepository;

  public ShortBranchOrPullRequestTrackerExecution(TrackerBaseInputFactory baseInputFactory, TrackerRawInputFactory rawInputFactory,
    TrackerMergeBranchInputFactory mergeInputFactory,
    Tracker<DefaultIssue, DefaultIssue> tracker, NewLinesRepository newLinesRepository) {
    this.baseInputFactory = baseInputFactory;
    this.rawInputFactory = rawInputFactory;
    this.mergeInputFactory = mergeInputFactory;
    this.tracker = tracker;
    this.newLinesRepository = newLinesRepository;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component) {
    Input<DefaultIssue> rawInput = rawInputFactory.create(component);
    Input<DefaultIssue> baseInput = baseInputFactory.create(component);

    Input<DefaultIssue> unmatchedRawInput;
    if (mergeInputFactory.hasMergeBranchAnalysis()) {
      Input<DefaultIssue> mergeInput = mergeInputFactory.create(component);
      Tracking<DefaultIssue, DefaultIssue> mergeTracking = tracker.trackNonClosed(rawInput, mergeInput);
      List<DefaultIssue> unmatchedRaws = mergeTracking.getUnmatchedRaws().collect(MoreCollectors.toList());
      unmatchedRawInput = new DefaultTrackingInput(unmatchedRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
    } else {
      List<DefaultIssue> filteredRaws = keepIssuesHavingAtLeastOneLocationOnChangedLines(component, rawInput.getIssues());
      unmatchedRawInput = new DefaultTrackingInput(filteredRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());
    }

    // do second tracking with base branch using raws issues that are still unmatched
    return tracker.trackNonClosed(unmatchedRawInput, baseInput);
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
