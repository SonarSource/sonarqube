/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue.workflow;

import java.util.List;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.rule.RuleType;
import org.sonar.server.issue.IssueFieldsSetter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@ServerSide
@ComputeEngineSide
public class IssueWorkflow {

  private final FunctionExecutor functionExecutor;
  private final IssueFieldsSetter updater;
  private final CodeQualityIssueWorkflow codeQualityIssueWorkflow;
  private final SecurityHostpotWorkflow securityHostpotWorkflow;

  public IssueWorkflow(FunctionExecutor functionExecutor, IssueFieldsSetter updater, CodeQualityIssueWorkflow codeQualityIssueWorkflow,
    SecurityHostpotWorkflow securityHostpotWorkflow) {
    this.functionExecutor = functionExecutor;
    this.updater = updater;
    this.codeQualityIssueWorkflow = codeQualityIssueWorkflow;
    this.securityHostpotWorkflow = securityHostpotWorkflow;
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).transition(transitionKey);
    if (transition.supports(issue) && !transition.automatic()) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
      updater.setIssueStatus(issue, previousIssueStatus, issue.issueStatus(), issueChangeContext);
      return true;
    }
    return false;
  }

  public List<Transition> outTransitions(DefaultIssue issue) {
    String status = issue.status();
    State state = getStateMachine(issue).state(status);
    checkArgument(state != null, "Unknown status: %s", status);
    return state.outManualTransitions(issue);
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).outAutomaticTransition(issue);
    if (transition != null) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
      updater.setIssueStatus(issue, previousIssueStatus, issue.issueStatus(), issueChangeContext);
    }
  }

  private State stateOf(DefaultIssue issue) {
    String status = issue.status();
    State state = getStateMachine(issue).state(status);
    String issueKey = issue.key();
    checkState(state != null, "Unknown status: %s [issue=%s]", status, issueKey);
    return state;
  }

  private StateMachine getStateMachine(DefaultIssue issue) {
    return isSecurityHotspot(issue) ? securityHostpotWorkflow.getMachine() : codeQualityIssueWorkflow.getMachine();
  }

  private static boolean isSecurityHotspot(DefaultIssue issue) {
    return issue.type() == RuleType.SECURITY_HOTSPOT;
  }

}
