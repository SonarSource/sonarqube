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

import java.util.Set;
import org.sonar.api.issue.Issue;
import org.sonar.ce.task.projectanalysis.analysis.AnalysisMetadataHolder;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.core.util.stream.MoreCollectors;

public class TrackerExecution {

  private final TrackerBaseInputFactory baseInputFactory;
  private final TrackerRawInputFactory rawInputFactory;
  private final ClosedIssuesInputFactory closedIssuesInputFactory;
  private final Tracker<DefaultIssue, DefaultIssue> tracker;
  private final ComponentIssuesLoader componentIssuesLoader;
  private final AnalysisMetadataHolder analysisMetadataHolder;

  public TrackerExecution(TrackerBaseInputFactory baseInputFactory, TrackerRawInputFactory rawInputFactory,
    ClosedIssuesInputFactory closedIssuesInputFactory, Tracker<DefaultIssue, DefaultIssue> tracker,
    ComponentIssuesLoader componentIssuesLoader, AnalysisMetadataHolder analysisMetadataHolder) {
    this.baseInputFactory = baseInputFactory;
    this.rawInputFactory = rawInputFactory;
    this.closedIssuesInputFactory = closedIssuesInputFactory;
    this.tracker = tracker;
    this.componentIssuesLoader = componentIssuesLoader;
    this.analysisMetadataHolder = analysisMetadataHolder;
  }

  public Tracking<DefaultIssue, DefaultIssue> track(Component component) {
    Input<DefaultIssue> rawInput = rawInputFactory.create(component);
    Input<DefaultIssue> openBaseIssuesInput = baseInputFactory.create(component);
    NonClosedTracking<DefaultIssue, DefaultIssue> openIssueTracking = tracker.trackNonClosed(rawInput, openBaseIssuesInput);
    if (openIssueTracking.isComplete() || analysisMetadataHolder.isFirstAnalysis()) {
      return openIssueTracking;
    }

    Input<DefaultIssue> closedIssuesBaseInput = closedIssuesInputFactory.create(component);
    Tracking<DefaultIssue, DefaultIssue> closedIssuesTracking = tracker.trackClosed(openIssueTracking, closedIssuesBaseInput);

    // changes of closed issues need to be loaded in order to:
    // - compute right transition from workflow
    // - recover fields values from before they were closed
    Set<DefaultIssue> matchesClosedIssues = closedIssuesTracking.getMatchedRaws().values().stream()
      .filter(t -> Issue.STATUS_CLOSED.equals(t.getStatus()))
      .collect(MoreCollectors.toSet());
    componentIssuesLoader.loadLatestDiffChangesForReopeningOfClosedIssues(matchesClosedIssues);

    return closedIssuesTracking;
  }

}
