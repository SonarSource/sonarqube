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
package org.sonar.core.issue.workflow;

import org.picocontainer.Startable;
import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.condition.HasResolution;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueUpdater;

import java.util.List;

@BatchSide
@ServerSide
public class IssueWorkflow implements Startable {

  private final FunctionExecutor functionExecutor;
  private final IssueUpdater updater;
  private StateMachine machine;

  public IssueWorkflow(FunctionExecutor functionExecutor, IssueUpdater updater) {
    this.functionExecutor = functionExecutor;
    this.updater = updater;
  }

  @Override
  public void start() {
    StateMachine.Builder builder = StateMachine.builder()
      // order is important for UI
      .states(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED);

    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    machine = builder.build();
  }

  private void buildManualTransitions(StateMachine.Builder builder) {
    builder.transition(Transition.builder(DefaultTransitions.CONFIRM)
      .from(Issue.STATUS_OPEN).to(Issue.STATUS_CONFIRMED)
      .functions(new SetResolution(null))
      .build())
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CONFIRMED)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.UNCONFIRM)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .conditions(new IsManual(true))
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(null), new SetCloseDate(false))
        .build())

      // resolve as false-positive
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())

      // resolve as won't fix
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), SetAssignee.UNASSIGN)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build()
      );

  }

  private void buildAutomaticTransitions(StateMachine.Builder builder) {
    // Close the "end of life" issues (disabled/deleted rule, deleted component)
    builder.transition(Transition.builder("automaticclose")
      .from(Issue.STATUS_OPEN).to(Issue.STATUS_CLOSED)
      .conditions(new IsEndOfLife(true))
      .functions(new SetEndOfLife(), new SetCloseDate(true))
      .automatic()
      .build())
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CLOSED)
        .conditions(new IsEndOfLife(true))
        .functions(new SetEndOfLife(), new SetCloseDate(true))
        .automatic()
        .build())
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_CLOSED)
        .conditions(new IsEndOfLife(true))
        .functions(new SetEndOfLife(), new SetCloseDate(true))
        .automatic()
        .build())
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_CLOSED)
        .conditions(new IsEndOfLife(true))
        .functions(new SetEndOfLife(), new SetCloseDate(true))
        .automatic()
        .build())
      .transition(Transition.builder("automaticclosemanual")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_CLOSED)
        .conditions(new IsEndOfLife(false), new IsManual(true))
        .functions(new SetCloseDate(true))
        .automatic()
        .build())

      // Reopen issues that are marked as resolved but that are still alive.
      // Manual issues are kept resolved.
      .transition(Transition.builder("automaticreopen")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(new IsEndOfLife(false), new HasResolution(Issue.RESOLUTION_FIXED), new IsManual(false))
        .functions(new SetResolution(null), new SetCloseDate(false))
        .automatic()
        .build()
      );
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public boolean doTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).transition(transitionKey);
    if (transition != null && !transition.automatic()) {
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
      return true;
    }
    return false;
  }

  public List<Transition> outTransitions(Issue issue) {
    State state = machine.state(issue.status());
    if (state == null) {
      throw new IllegalArgumentException("Unknown status: " + issue.status());
    }
    return state.outManualTransitions(issue);
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).outAutomaticTransition(issue);
    if (transition != null) {
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
    }
  }

  public List<String> statusKeys() {
    return machine.stateKeys();
  }

  private State stateOf(DefaultIssue issue) {
    State state = machine.state(issue.status());
    if (state == null) {
      throw new IllegalStateException("Unknown status: " + issue.status() + " [issue=" + issue.key() + "]");
    }
    return state;
  }

  StateMachine machine() {
    return machine;
  }
}
