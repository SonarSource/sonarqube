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
package org.sonar.server.issue.workflow.securityhotspot;

import java.util.List;
import java.util.function.Consumer;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.issue.workflow.statemachine.State;
import org.sonar.issue.workflow.statemachine.Transition;
import org.sonar.server.issue.workflow.issue.IssueWorkflowEntityAdapter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@ServerSide
@ComputeEngineSide
public class SecurityHotspotWorkflow {

  private final SecurityHotspotWorkflowActionsFactory actionsFactory;
  private final SecurityHotspotWorkflowDefinition workflowDefinition;

  public SecurityHotspotWorkflow(SecurityHotspotWorkflowActionsFactory actionsFactory, SecurityHotspotWorkflowDefinition workflowDefinition) {
    this.actionsFactory = actionsFactory;
    this.workflowDefinition = workflowDefinition;
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> transition = stateOf(issue).transition(transitionKey);
    var wfIssue = adapt(issue);
    if (transition.supports(wfIssue) && !transition.automatic()) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      SecurityHotspotWorkflowActions issueActions = actionsFactory.provideContextualActions(issue, issueChangeContext);
      execute(transition.actions(), issueActions);
      issueActions.setStatus(previousIssueStatus, transition.to());
      return true;
    }
    return false;
  }

  public List<String> outTransitionsKeys(DefaultIssue issue) {
    String status = issue.status();
    State<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> state = workflowDefinition.getMachine().state(status);
    checkArgument(state != null, "Unknown status: %s", status);
    return state.outManualTransitions(adapt(issue))
      .stream()
      .map(Transition::key)
      .toList();
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    var wfIssue = adapt(issue);
    Transition<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> transition = stateOf(issue).outAutomaticTransition(wfIssue);
    if (transition != null) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      SecurityHotspotWorkflowActions actions = actionsFactory.provideContextualActions(issue, issueChangeContext);
      execute(transition.actions(), actions);
      actions.setStatus(previousIssueStatus, transition.to());
    }
  }

  private State<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> stateOf(DefaultIssue issue) {
    State<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> state = workflowDefinition.getMachine().state(issue.status());
    checkState(state != null, "Unknown status: %s [issue=%s]", issue.status(), issue.key());
    return state;
  }

  void execute(List<Consumer<SecurityHotspotWorkflowActions>> actions, SecurityHotspotWorkflowActions issueActions) {
    if (!actions.isEmpty()) {
      for (Consumer<SecurityHotspotWorkflowActions> action : actions) {
        action.accept(issueActions);
      }
    }
  }

  private static IssueWorkflowEntityAdapter adapt(DefaultIssue issue) {
    return new IssueWorkflowEntityAdapter(issue);
  }
}
