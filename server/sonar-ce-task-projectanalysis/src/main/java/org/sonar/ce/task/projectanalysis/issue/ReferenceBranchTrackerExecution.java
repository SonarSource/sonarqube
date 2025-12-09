/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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

import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;

public class ReferenceBranchTrackerExecution {
  private final TrackerReferenceBranchInputFactory referenceBranchInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;

  public ReferenceBranchTrackerExecution(TrackerReferenceBranchInputFactory referenceBranchInputFactory, Tracker<DefaultIssue, DefaultIssue> tracker) {
    this.referenceBranchInputFactory = referenceBranchInputFactory;
    this.tracker = tracker;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component, Input<DefaultIssue> rawInput) {
    return tracker.trackNonClosed(rawInput, referenceBranchInputFactory.create(component));
  }
}
