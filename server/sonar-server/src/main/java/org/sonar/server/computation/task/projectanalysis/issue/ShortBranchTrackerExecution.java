/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import java.util.ArrayList;
import java.util.List;

import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.server.computation.task.projectanalysis.component.Component;

public class ShortBranchTrackerExecution {
  private final TrackerBaseInputFactory baseInputFactory;
  private final TrackerRawInputFactory rawInputFactory;
  private final TrackerMergeBranchInputFactory mergeInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;

  public ShortBranchTrackerExecution(TrackerBaseInputFactory baseInputFactory, TrackerRawInputFactory rawInputFactory, TrackerMergeBranchInputFactory mergeInputFactory,
    Tracker<DefaultIssue, DefaultIssue> tracker) {
    this.baseInputFactory = baseInputFactory;
    this.rawInputFactory = rawInputFactory;
    this.mergeInputFactory = mergeInputFactory;
    this.tracker = tracker;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component) {
    Input<DefaultIssue> rawInput = rawInputFactory.create(component);
    Input<DefaultIssue> baseInput = baseInputFactory.create(component);
    Input<DefaultIssue> mergeInput = mergeInputFactory.create(component);

    Tracking<DefaultIssue, DefaultIssue> mergeTracking = tracker.track(rawInput, mergeInput);
    List<DefaultIssue> unmatchedRaws = toList(mergeTracking.getUnmatchedRaws());
    Input<DefaultIssue> unmatchedRawInput = new DefaultTrackingInput(unmatchedRaws, rawInput.getLineHashSequence(), rawInput.getBlockHashSequence());

    // do second tracking with base branch using raws issues that are still unmatched
    return tracker.track(unmatchedRawInput, baseInput);
  }

  private static <T> List<T> toList(Iterable<T> iterable) {
    List<T> list = new ArrayList<>();
    iterable.forEach(list::add);
    return list;
  }
}
