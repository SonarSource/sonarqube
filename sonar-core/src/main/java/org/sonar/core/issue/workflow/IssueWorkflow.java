/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.issue.workflow;

import org.picocontainer.Startable;
import org.sonar.api.BatchComponent;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.core.issue.DefaultIssue;

import java.util.List;
import java.util.Map;

public class IssueWorkflow implements BatchComponent, ServerComponent, Startable {

  private StateMachine machine;

  @Override
  public void start() {
    machine = StateMachine.builder()
      .states(Issue.STATUS_OPEN, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED)
      .transition(Transition.builder(DefaultTransitions.CLOSE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_CLOSED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
          // TODO set closed at
        .build())
      .transition(Transition.builder(DefaultTransitions.CLOSE)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_CLOSED)
          // TODO set closed at
        .build())
      .transition(Transition.builder(DefaultTransitions.CLOSE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CLOSED)
          // TODO set closed at
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(Issue.RESOLUTION_OPEN))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(Issue.RESOLUTION_OPEN))
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(new IsNotManualIssue())
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE))
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(new IsNotManualIssue())
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE))
        .build())
      .build();
  }

  @Override
  public void stop() {
  }

  public List<Transition> availableTransitions(Issue issue) {
    return machine.state(issue.status()).outTransitions(issue);
  }

  public boolean apply(DefaultIssue issue, IssueChange change) {
    if (change.hasChanges()) {
      if (change.description() != null) {
        issue.setDescription(change.description());
      }
      if (change.manualSeverity() != null) {
        change.setManualSeverity(change.manualSeverity());
      }
      if (change.severity() != null) {
        issue.setSeverity(change.severity());
      }
      if (change.title() != null) {
        issue.setTitle(change.title());
      }
      if (change.isAssigneeChanged()) {
        issue.setAssignee(change.assignee());
      }
      if (change.isLineChanged()) {
        issue.setLine(change.line());
      }
      if (change.isCostChanged()) {
        issue.setCost(change.cost());
      }
      for (Map.Entry<String, String> entry : change.attributes().entrySet()) {
        issue.setAttribute(entry.getKey(), entry.getValue());
      }
      if (change.transition() != null) {
        move(issue, change.transition());
      }
      return true;
    }
    return false;
  }

  public void move(DefaultIssue issue, String transition) {
    State state = machine.state(issue.status());
    state.move(issue, transition);
  }
}
