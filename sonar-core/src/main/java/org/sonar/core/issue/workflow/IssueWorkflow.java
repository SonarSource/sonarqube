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
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;

import java.util.List;

public class IssueWorkflow implements BatchComponent, ServerComponent, Startable {

  private StateMachine machine;
  private final FunctionExecutor functionExecutor;

  public IssueWorkflow(FunctionExecutor functionExecutor) {
    this.functionExecutor = functionExecutor;
  }

  @Override
  public void start() {
    machine = StateMachine.builder()
      .states(Issue.STATUS_OPEN, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED)
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
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_REOPENED)
        .functions(new SetResolution(null), new SetCloseDate(false))
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(new IsManual(false))
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE))
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(new IsManual(false))
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE))
        .build())

        // automatic transitions

        // Close the issues that do not exist anymore. Note that isAlive() is true on manual issues
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_CLOSED)
        .conditions(new IsAlive(false), new IsManual(false))
        .functions(new SetResolution(Issue.RESOLUTION_FIXED), new SetCloseDate(true))
        .automatic()
        .build())
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CLOSED)
        .conditions(new IsAlive(false))
        .functions(new SetResolution(Issue.RESOLUTION_FIXED), new SetCloseDate(true))
        .automatic()
        .build())
        // Close the issues marked as resolved and that do not exist anymore.
        // Note that false-positives are kept resolved and are not closed.
      .transition(Transition.builder("automaticclose")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_CLOSED)
        .conditions(new IsAlive(false))
        .functions(new SetCloseDate(true))
        .automatic()
        .build())
      .transition(Transition.builder("automaticreopen")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(new IsAlive(true), new HasResolution(Issue.RESOLUTION_FIXED))
        .functions(new SetResolution(null))
        .automatic()
        .build())
      .build();
  }

  @Override
  public void stop() {
  }

  public boolean doTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).transition(transitionKey);
    if (transition != null && !transition.automatic()) {
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      issue.setStatus(transition.to());
      return true;
    }
    return false;
  }

  public List<Transition> outTransitions(Issue issue) {
    return machine.state(issue.status()).outManualTransitions(issue);
  }


  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).outAutomaticTransition(issue);
    if (transition != null) {
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      issue.setStatus(transition.to());
    }
  }

  private State stateOf(DefaultIssue issue) {
    State state = machine.state(issue.status());
    if (state==null) {
      throw new IllegalStateException("Unknown status: " + issue.status() + " [issue=" + issue.key() + "]");
    }
    return state;
  }

  StateMachine machine() {
    return machine;
  }
}
